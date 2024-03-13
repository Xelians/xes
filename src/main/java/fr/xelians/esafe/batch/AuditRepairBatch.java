/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
    if (serverNodeService.hasFeature(NodeFeature.AUDIT_REPAIR)) {
      // Resynchronize operation and logbook index (ingest in stored state & update )
      // audit a resynchronize offers
    }
  }
}
