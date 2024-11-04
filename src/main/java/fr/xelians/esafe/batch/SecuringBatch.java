/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.service.ClusterService;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.operation.service.SecuringService;
import fr.xelians.esafe.organization.service.TenantService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecuringBatch {

  private static final int LBK_MAX_SIZE = 100_000;
  private static final Duration PT24H = Duration.ofHours(24);

  private final ClusterService clusterService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final SecuringService securingService;

  @Value("${app.batch.secure.fixedDelay:PT3600S}")
  private Duration fixedDelay;

  private Duration maxDelay;

  @PostConstruct
  public void init() {
    maxDelay = PT24H.minus(fixedDelay);
    log.info("Starting securing batch - fixedDelay: {} - maxDelay: {}", fixedDelay, maxDelay);
  }

  @Scheduled(
      fixedDelayString = "${app.batch.secure.fixedDelay:PT3600S}",
      initialDelayString = "${app.batch.secure.initialDelay:PT1S}")
  public void run() {
    try {
      if (clusterService.isActive(JobType.TRACEABILITY)) {
        secureOperations();
      }
    } catch (Exception ex) {
      log.error("Secure operation batch failed", ex);
    }
  }

  // TODO détruire les operations qui sécurisées sur l'offre de stockage

  private void secureOperations() {
    LocalDateTime now = LocalDateTime.now().minusMinutes(0);
    log.debug("Batch start " + now);

    // Fetch operations in database that need securing before now date sorted by tenant and id
    List<OperationDb> ops = operationService.findForSecuring(-1L, -1L, now);
    if (ops.isEmpty()) {
      return;
    }

    Long tenant = ops.getFirst().getTenant();
    Long lbkId = -1L;
    int size = 0;
    List<OperationDb> operations = new ArrayList<>();

    do {
      for (OperationDb operation : ops) {
        // Secure operations if tenant changes
        if (!Objects.equals(operation.getTenant(), tenant)) {
          if (!operations.isEmpty()) {
            secureOperations(tenant, operations, now);
            operations = new ArrayList<>();
            size = 0;
          }
          tenant = operation.getTenant();
        }

        // Add operation
        operations.add(operation);
        lbkId = operation.getLbkId();
        size += 1 + operation.getActions().size();

        // Secure operations if size is too big
        if (size > LBK_MAX_SIZE) {
          secureOperations(tenant, operations, now);
          operations = new ArrayList<>();
          size = 0;
        }
      }

      // As the findOperationsToSecure fetches at max 1000 operations, we can stop as soon this
      // number is not reached
      if (ops.size() < OperationService.MAX_1000) {
        if (!operations.isEmpty()) secureOperations(tenant, operations, now);
        return;
      }

      // Fetch next operations in database
      ops = operationService.findForSecuring(tenant, lbkId, now);

    } while (!ops.isEmpty());
  }

  private void secureOperations(
      Long tenant, List<OperationDb> operations, LocalDateTime startDate) {

    // Don't create a new securing operation if this is not necessary
    if (isSecuringOperations(operations)) {
      OperationDb lastOp = operations.getLast();
      LocalDateTime lastDate = lastOp.getCreated();
      Duration delay = Duration.between(lastDate, startDate);
      if (delay.compareTo(maxDelay) < 0) {
        return;
      }
    }

    // Create a new securing operation
    OperationDb securingOp = OperationFactory.securingOp(tenant);
    securingOp.setStatus(OperationStatus.RUN);
    securingOp.setMessage("Securing operations");
    securingOp = operationService.save(securingOp);

    // Write operations to logbook
    List<String> offers = tenantService.getTenantDb(tenant).getStorageOffers();
    if (offers.isEmpty()) {
      return;
    }

    long secureNum = securingService.writeLbk(securingOp, operations, offers, Hash.SHA256);

    // TODO Check when starting the batch that operationSe into lbk and index are coherents
    List<LogbookOperation> ops =
        operations.stream()
            .map(OperationDb::toOperationSe)
            .peek(o -> o.setSecureNumber(secureNum))
            .toList();

    // TODO we must retry to index if indexation failed
    // Index securing operation and operations with the secureNumber
    securingService.index(securingOp, ops);

    // Delete .ope on offers
    securingService.deleteOpe(offers, ops);
  }

  // Note. a logbook can contain zero or n securing operations
  private boolean isSecuringOperations(List<OperationDb> operations) {
    return operations.stream().noneMatch(o -> o.getType() != OperationType.TRACEABILITY);
  }
}
