/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.batch;

import static fr.xelians.esafe.operation.domain.OperationStatus.*;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.service.ServerNodeService;
import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.service.OperationService;
import jakarta.validation.constraints.Min;
import java.nio.file.Path;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CleanOperationBatch {

  private final ServerNodeService serverNodeService;
  private final OperationService operationService;

  // Two days
  @Value("${app.batch.clean.running:48}")
  @Min(1)
  private int runningHours;

  // Two days
  @Value("${app.batch.clean.succeeded:48}")
  @Min(1)
  private int succeededHours;

  // Seven days
  @Value("${app.batch.clean.failed:168}")
  @Min(1)
  private int failedHours;

  // second, minute, hour, day of month, month, day(s) of week
  // * means match any
  // */X means "every X"
  // ? ("no specific value") - when you need to specify something in one of the two fields in which
  // the character is allowed, but not the other.
  @Scheduled(cron = "${app.batch.clean.cron:0 0 3 * * ?}")
  public void run() {

    if (serverNodeService.hasFeature(NodeFeature.CLEAN_OPERATION)) {

      // An application crash during INIT or RUN phase can yield dangling operations in database
      LocalDateTime runningDate = LocalDateTime.now().minusHours(runningHours);
      updateOperation(INIT, runningDate, ERROR_INIT, "Init Timeout");
      updateOperation(RUN, runningDate, ERROR_COMMIT, "Run Timeout");

      // Remove succeeded and secured operations
      LocalDateTime securedDate = LocalDateTime.now().minusHours(succeededHours);
      deleteSecuredOperation(OK, securedDate);

      // Remove not recoverable error operations
      LocalDateTime failedDate = LocalDateTime.now().minusHours(failedHours);
      deleteFailedOperation(ERROR_INIT, failedDate);
      deleteFailedOperation(ERROR_CHECK, failedDate);
      deleteFailedOperation(ERROR_COMMIT, failedDate);
      deleteFailedOperation(FATAL, failedDate);
    }
  }

  private void updateOperation(
      OperationStatus status, LocalDateTime date, OperationStatus newStatus, String newDetail) {
    try {
      operationService.updateByStatusAndDate(status, date, newStatus, newDetail);
    } catch (Exception ex) {
      log.error("Update init or run operation error", ex);
    }
  }

  private void deleteSecuredOperation(OperationStatus status, LocalDateTime date) {
    try {
      operationService.deleteSecuredOperations(status, date);
    } catch (Exception ex) {
      log.error("Delete secured operation error", ex);
    }
  }

  private void deleteFailedOperation(OperationStatus status, LocalDateTime date) {
    try {
      operationService.findIdByStatusAndDate(status, date).forEach(this::deleteOperation);
    } catch (Exception ex) {
      log.error("Delete failed operation error", ex);
    }
  }

  private void deleteOperation(Long operationId) {
    operationService.deleteOperations(operationId);
    Path ws = Workspace.getPath(operationId);
    NioUtils.deleteDirQuietly(ws);
  }
}
