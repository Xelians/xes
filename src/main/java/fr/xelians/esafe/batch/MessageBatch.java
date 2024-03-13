/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
