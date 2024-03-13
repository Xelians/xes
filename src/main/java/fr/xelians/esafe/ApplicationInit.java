/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe;

import fr.xelians.esafe.admin.service.IndexAdminService;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(0)
public class ApplicationInit implements ApplicationListener<ApplicationReadyEvent> {

  private final IndexAdminService indexAdminService;

  @Autowired
  public ApplicationInit(IndexAdminService indexAdminService) {
    this.indexAdminService = indexAdminService;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("ApplicationListener#onApplicationEvent()");

    try {
      indexAdminService.deleteIndex();
      indexAdminService.resetIndex();

    } catch (IOException e) {
      log.error("Failed to create LogbookOperation index", e);
      log.error("Exiting...");
      System.exit(1);
    }
  }
}
