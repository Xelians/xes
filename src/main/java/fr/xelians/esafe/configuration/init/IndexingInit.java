/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration.init;

import fr.xelians.esafe.admin.service.IndexAdminService;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Component
@AllArgsConstructor
public class IndexingInit implements ApplicationListener<ApplicationReadyEvent> {

  private final IndexAdminService indexAdminService;
  private final IndexingProperties indexingProperties;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("ApplicationListener#onApplicationEvent()");
    createIndex();
    resetIndex();
  }

  private void resetIndex() {
    if (BooleanUtils.isTrue(indexingProperties.isReset())) {
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
    if (BooleanUtils.isTrue(indexingProperties.isCreateIfMissing())) {
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
