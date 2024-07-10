/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe;

import fr.xelians.esafe.admin.service.IndexAdminService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class ApplicationInit implements ApplicationListener<ApplicationReadyEvent> {

  private final IndexAdminService indexAdminService;

  @Value("${app.indexing.on-start.reset:false}")
  private Boolean resetIndex;

  @Value("${app.indexing.on-start.create-if-missing:true}")
  private Boolean createIndexIfMissing;

  @Autowired
  public ApplicationInit(IndexAdminService indexAdminService) {
    this.indexAdminService = indexAdminService;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("ApplicationListener#onApplicationEvent()");
    createIndex();
    resetIndex();
  }

  private void resetIndex() {
    if (BooleanUtils.isTrue(this.resetIndex)) {
      try {
        indexAdminService.deleteAllIndex();
        indexAdminService.resetAllIndex();
      } catch (IOException e) {
        log.error("Failed to create LogbookOperation index", e);
        shutdownWithError();
      }
    }
  }

  private void createIndex() {
    if (BooleanUtils.isTrue(this.createIndexIfMissing)) {
      try {
        indexAdminService.createAllIndex();
      } catch (IOException e) {
        log.error("Failed to create missing index", e);
        shutdownWithError();
      }
    }
  }

  private static void shutdownWithError() {
    log.error("Exiting...");
    System.exit(1);
  }
}
