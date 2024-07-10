/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageBatch {

  private final MessageService messageService;

  @Scheduled(
      fixedDelayString = "${app.batch.servernode.fixedDelay:PT5s}",
      initialDelayString = "${app.batch.servernode.initialDelay:PT1s}")
  public void run() {
    messageService.process();
  }
}
