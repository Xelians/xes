/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.batch;

import fr.xelians.esafe.cluster.service.ServerNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerNodeBatch {

  private final ServerNodeService serverNodeService;

  @Scheduled(
      fixedDelayString = "${app.batch.servernode.fixedDelay:60000}",
      initialDelayString = "${app.batch.servernode.initialDelay:0}")
  public void run() {
    try {
      serverNodeService.updateFeatures();
    } catch (Exception ex) {
      log.error("Server Node Batch failed", ex);
    }
  }
}
