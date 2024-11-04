/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.service.ClusterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClusterBatch {

  private final ClusterService clusterService;

  @Scheduled(
      fixedDelayString = "${app.batch.cluster.fixedDelay:120000}",
      initialDelayString = "${app.batch.cluster.initialDelay:0}")
  public void run() {
    try {
      clusterService.refreshJobs();
    } catch (Exception ex) {
      log.error("Cluster batch failed", ex);
    }
  }
}
