/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.service.ServerNodeService;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.entity.OperationSe;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class SecuringBatch {

  public static final int LBK_MAX_SIZE = 100_000;

  private static final Duration PT24H = Duration.ofHours(24);

  private final ServerNodeService serverNodeService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final SecuringService securingService;

  @Value("${app.batch.secure.fixedDelay:PT3600S}")
  private Duration fixedDelay;

  private Duration maxDelay;

  @PostConstruct
  public void init() {
    maxDelay = PT24H.minus(fixedDelay);
  }

  @Scheduled(
      fixedDelayString = "${app.batch.secure.fixedDelay:PT3600S}",
      initialDelayString = "${app.batch.secure.initialDelay:PT1S}")
  public void run() {
    try {
      if (serverNodeService.hasFeature(NodeFeature.SECURE_OPERATION)) {
        secureOperations();
      }
    } catch (Exception ex) {
      log.error("Secure operation batch failed", ex);
    }
  }

  // TODO détruire les operations qui sécurisées sur l'offre de stockage

  private void secureOperations() {
    LocalDateTime startDate = LocalDateTime.now().minusMinutes(0);

    // Fetch operations in database that need securing sorted by tenant, id and before start date
    List<OperationDb> ops = operationService.findOperationsToSecure(-1L, -1L, startDate);
    if (ops.isEmpty()) return;

    Long tenant = ops.getFirst().getTenant();
    Long lbkId = -1L;
    int size = 0;
    List<OperationDb> operations = new ArrayList<>();

    do {
      for (OperationDb operation : ops) {

        // Secure operations if tenant changes
        if (!Objects.equals(operation.getTenant(), tenant)) {
          if (!operations.isEmpty()) {
            secureOperations(tenant, operations, startDate);
            operations = new ArrayList<>();
            size = 0;
          }
          tenant = operation.getTenant();
        }

        // Add operations that were created before startDate
        operations.add(operation);
        lbkId = operation.getLbkId();
        size += 1 + operation.getActions().size();

        // Secure operations if size is too big
        if (size > LBK_MAX_SIZE) {
          secureOperations(tenant, operations, startDate);
          operations = new ArrayList<>();
          size = 0;
        }
      }

      // As the findOperationsToSecure fetches at max 1000 operations, we can stop as soon this
      // number is not reached
      if (ops.size() < OperationService.MAX_1000) {
        if (!operations.isEmpty()) secureOperations(tenant, operations, startDate);
        return;
      }

      // Fetch next operations in database
      ops = operationService.findOperationsToSecure(tenant, lbkId, startDate);

    } while (!ops.isEmpty());
  }

  private void secureOperations(
      Long tenant, List<OperationDb> operations, LocalDateTime startDate) {

    // Don't create a new securing operation if this is not nécessary
    if (isOnlySecuringOperation(operations)) {
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
    long secureNum = securingService.writeLbk(securingOp, operations, offers, Hash.SHA256);

    List<OperationSe> ops =
        operations.stream()
            .map(OperationDb::toOperationSe)
            .peek(o -> o.setSecureNumber(secureNum))
            .toList();

    // Index securing operation and operations with the secureNumber
    securingService.index(securingOp, ops);

    // Delete .ope on offers
    securingService.deleteOpe(offers, ops);
  }

  // Note. a logbook can contain zero or n secure operations
  private boolean isOnlySecuringOperation(List<OperationDb> operations) {
    return operations.stream().noneMatch(o -> o.getType() != OperationType.SECURING);
  }
}
