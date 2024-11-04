/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import static fr.xelians.esafe.operation.domain.OperationStatus.*;

import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.service.ClusterService;
import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.service.OperationService;
import jakarta.validation.constraints.Min;
import java.nio.file.Path;
import java.time.LocalDateTime;
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
public class CleanOperationBatch {

  private final ClusterService clusterService;
  private final OperationService operationService;

  // Two days
  @Value("${app.batch.clean.running:48}")
  @Min(1)
  private int runningHours;

  // Two days
  @Value("${app.batch.clean.succeeded:48}")
  @Min(1)
  private int succeededHours;

  // Seven days
  @Value("${app.batch.clean.failed:168}")
  @Min(1)
  private int failedHours;

  // second, minute, hour, day of month, month, day(s) of week
  // * means match any
  // */X means "every X"
  // ? ("no specific value") - when you need to specify something in one of the two fields in which
  // the character is allowed, but not the other.
  @Scheduled(cron = "${app.batch.clean.cron:0 0 3 * * ?}")
  public void run() {

    if (clusterService.isActive(JobType.CLEAN)) {

      // An application crash during INIT or RUN phase can yield dangling operations in database
      LocalDateTime runningDate = LocalDateTime.now().minusHours(runningHours);
      updateOperation(INIT, runningDate, ERROR_INIT, "Init Timeout");
      updateOperation(RUN, runningDate, ERROR_COMMIT, "Run Timeout");

      // Remove succeeded and secured operations
      LocalDateTime succeededDate = LocalDateTime.now().minusHours(succeededHours);
      deleteSucceededOperations(succeededDate);

      // Remove not recoverable error operations
      LocalDateTime failedDate = LocalDateTime.now().minusHours(failedHours);
      deleteFailedOperations(failedDate);
    }
  }

  private void updateOperation(
      OperationStatus status, LocalDateTime date, OperationStatus newStatus, String newDetail) {
    try {
      operationService.updateByStatusAndDate(status, date, newStatus, newDetail);
    } catch (Exception ex) {
      log.error("Update init or run operation error", ex);
    }
  }

  private void deleteSucceededOperations(LocalDateTime date) {
    try {
      operationService.findCompletedOperationIds(OK, date).forEach(this::deleteOperation);
    } catch (Exception ex) {
      log.error("Failed to delete succeed operation", ex);
    }
  }

  private void deleteFailedOperations(LocalDateTime date) {
    for (var status : FAILED_STATUS)
      try {
        operationService.findOperationIds(status, date).forEach(this::deleteOperation);
      } catch (Exception ex) {
        log.error("Failed to delete failed operation", ex);
      }
  }

  // TODO Delete on each node
  // TODO Delete DIP on serveur
  private void deleteOperation(Long operationId) {
    operationService.deleteOperations(operationId);
    Path ws = Workspace.getPath(operationId);
    NioUtils.deleteDirQuietly(ws);
  }
}
