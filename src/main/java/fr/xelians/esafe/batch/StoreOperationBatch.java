/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import static java.util.stream.Collectors.groupingBy;

import fr.xelians.esafe.archive.service.*;
import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.service.ClusterService;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.operation.service.SecuringService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * All methods in this class must be idempotent
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreOperationBatch {

  public static final String BAD_OPERATION_STATUS = "Bad operation '%s' - status '%s'";
  // TODO configure CPUs number
  private static final ForkJoinPool RUN_POOL = new ForkJoinPool(Utils.CPUS);

  private final ClusterService clusterService;
  private final OperationService operationService;
  private final ReclassificationService reclassificationService;
  private final EliminationService eliminationService;
  private final TransferService transferService;
  private final TransferReplyService transferReplyService;
  private final UpdateService updateService;
  private final UpdateRulesService updateRulesService;
  private final IndexService unitIngestService;
  private final LogbookService logbookService;
  private final SecuringService securingService;

  // Execute every second
  @Scheduled(
      fixedDelayString = "${app.batch.store.operation.fixedDelay:2000}",
      initialDelayString = "${app.batch.store.operation.initialDelay:2000}")
  public void run() {
    try {
      // TODO affecter 1 thread par Tenant et ne pas bloquer les autres tenants si une thread n'a
      // pas fini
      if (clusterService.isActive(JobType.STORE)) {
        Collection<List<OperationDb>> groupList =
            operationService.findByStatus(OperationStatus.STORE, OperationStatus.INDEX).stream()
                .collect(groupingBy(OperationDb::getTenant))
                .values();
        if (groupList.size() > 1) {
          RUN_POOL.submit(() -> groupList.parallelStream().forEach(this::selectOperation)).get();
        } else {
          groupList.forEach(this::selectOperation);
        }
      }
    } catch (Exception ex) {
      log.error("Store batch failed", ex);
    }
  }

  // TODO :  prendre en charge les operations referentielles / user / orga / tenant (faire un 2ème
  // batch avec un délai plus grand ?)
  private void selectOperation(List<OperationDb> operations) {
    for (OperationDb operation : operations) {
      switch (operation.getType()) {
        case UPDATE_ARCHIVE -> updateArchive(operation);
        case UPDATE_ARCHIVE_RULES -> updateArchiveRules(operation);
        case RECLASSIFY_ARCHIVE -> reclassifyArchive(operation);
        case ELIMINATE_ARCHIVE -> eliminateArchive(operation);
        case TRANSFER_ARCHIVE -> transferArchive(operation);
        case TRANSFER_REPLY -> transferReply(operation);
        case INGEST_ARCHIVE, INGEST_FILING, INGEST_HOLDING -> unitIngestService.indexOperation(
            operation);
        case TRACEABILITY -> indexSecuring(operation);
        case EXTERNAL,
            CREATE_ACCESSCONTRACT,
            UPDATE_ACCESSCONTRACT,
            CREATE_AGENCY,
            UPDATE_AGENCY,
            CREATE_ONTOLOGY,
            UPDATE_ONTOLOGY,
            CREATE_INGESTCONTRACT,
            UPDATE_INGESTCONTRACT,
            CREATE_ORGANIZATION,
            UPDATE_ORGANIZATION,
            CREATE_PROFILE,
            UPDATE_PROFILE,
            CREATE_RULE,
            UPDATE_RULE,
            CREATE_TENANT,
            UPDATE_TENANT,
            CREATE_USER,
            UPDATE_USER -> logbookService.indexOperation(operation);

        default -> throw new InternalException(
            "Select operation batch failed",
            String.format(
                "Bad operation '%s' - type '%s'", operation.getId(), operation.getType()));
      }
    }
  }

  private void indexSecuring(OperationDb operation) {
    if (operation.getStatus() == OperationStatus.INDEX) {
      securingService.indexOperation(operation);
    } else {
      throw new InternalException(
          "Secured operation batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void updateArchive(OperationDb operation) {
    switch (operation.getStatus()) {
      case STORE -> updateService.storeOperation(operation);
      case INDEX -> updateService.indexOperation(operation);
      default -> throw new InternalException(
          "Update archive batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void updateArchiveRules(OperationDb operation) {
    switch (operation.getStatus()) {
      case STORE -> updateRulesService.storeOperation(operation);
      case INDEX -> updateRulesService.indexOperation(operation);
      default -> throw new InternalException(
          "Update archive rules batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void reclassifyArchive(OperationDb operation) {
    switch (operation.getStatus()) {
      case STORE -> reclassificationService.storeOperation(operation);
      case INDEX -> reclassificationService.indexOperation(operation);
      default -> throw new InternalException(
          "Eliminate archive batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void eliminateArchive(OperationDb operation) {
    if (operation.getStatus() == OperationStatus.STORE) {
      eliminationService.storeOperation(operation);
    } else {
      throw new InternalException(
          "Eliminate archive batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void transferArchive(OperationDb operation) {
    if (operation.getStatus() == OperationStatus.STORE) {
      transferService.storeOperation(operation);
    } else {
      throw new InternalException(
          "Transfer archive batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }

  private void transferReply(OperationDb operation) {
    if (operation.getStatus() == OperationStatus.STORE) {
      transferReplyService.storeOperation(operation);
    } else {
      throw new InternalException(
          "Transfer reply archive batch failed",
          String.format(BAD_OPERATION_STATUS, operation.getId(), operation.getStatus()));
    }
  }
}
