/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.domain.JobType;
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
public class AuditRepairBatch {

  private final ClusterService clusterService;

  @Scheduled(cron = "${app.batch.clean.cron:0 0 3 * * ?}")
  public void run() {
    if (clusterService.isActive(JobType.AUDIT)) {
      // Resynchronize operation and logbook index (ingest in stored state & update )
      // audit a resynchronize offers
    }
  }
}
