/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.service.ServerNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRepairBatch {

  private final ServerNodeService serverNodeService;

  @Scheduled(cron = "${app.batch.clean.cron:0 0 3 * * ?}")
  public void run() {
    if (serverNodeService.hasFeature(NodeFeature.AUDIT)) {
      // Resynchronize operation and logbook index (ingest in stored state & update )
      // audit a resynchronize offers
    }
  }
}
