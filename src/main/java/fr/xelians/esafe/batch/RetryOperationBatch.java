/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.service.ServerNodeService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.service.OperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryOperationBatch {

  private final ServerNodeService serverNodeService;
  private final OperationService operationService;

  @Scheduled(
      fixedDelayString = "${app.batch.retry.fixedDelay:900000}",
      initialDelayString = "${app.batch.retry.initialDelay:180000}")
  public void run() {
    try {
      if (serverNodeService.hasFeature(NodeFeature.RETRY)) {
        operationService.updateStatusAndMessage(
            OperationStatus.RETRY_STORE, OperationStatus.STORE, "Retry storing");
        operationService.updateStatusAndMessage(
            OperationStatus.RETRY_INDEX, OperationStatus.INDEX, "Retry indexing");
      }
    } catch (Exception ex) {
      log.error("Retry Operation Batch failed", ex);
    }
  }
}
