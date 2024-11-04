/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.accession.service.AccessionRegisterService;
import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.service.ClusterService;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
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
public class AccessionRegisterBatch {

  private final ClusterService clusterService;
  private final OperationService operationService;
  private final AccessionRegisterService accessionRegisterService;

  @Value("${app.batch.accession.fixedDelay:PT600S}")
  private Duration fixedDelay;

  @PostConstruct
  public void init() {
    log.info("Starting accession batch - fixedDelay: {}", fixedDelay);
  }

  @Scheduled(
      fixedDelayString = "${app.batch.accession.fixedDelay:PT600S}",
      initialDelayString = "${app.batch.accession.initialDelay:PT1S}")
  public void run() {
    try {
      if (clusterService.isActive(JobType.ACCESSION)) {
        List<OperationDb> ops = operationService.findToRegister();
        if (!ops.isEmpty()) {
          accessionRegisterService.registerOperations(ops);
        }
      }
    } catch (Exception ex) {
      log.error("Accession batch failed", ex);
    }
  }
}
