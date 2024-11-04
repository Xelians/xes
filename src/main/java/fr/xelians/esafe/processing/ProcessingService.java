/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.processing;

import fr.xelians.esafe.common.task.AbstractOperationTask;
import fr.xelians.esafe.operation.service.OperationService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

  @Getter private final OperationService operationService;

  private final MeterRegistry meterRegistry;

  @Value("${app.processing.threads:0}")
  private int processingThreads;

  private TaskPoolExecutor executor;

  @PostConstruct
  public void init() {
    int defaultThreads = Runtime.getRuntime().availableProcessors() * 2;
    int threads = processingThreads <= 0 ? defaultThreads : processingThreads;
    log.info("Number thread available={} for task pool executor", threads);
    this.executor = new TaskPoolExecutor(operationService, threads, meterRegistry);
    this.executor.start();
  }

  @PreDestroy
  public void destroy() {
    this.executor.stop();
  }

  public Future<?> submit(AbstractOperationTask task) {
    return executor.submit(task);
  }
}
