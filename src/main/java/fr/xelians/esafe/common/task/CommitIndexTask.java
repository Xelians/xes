/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import static fr.xelians.esafe.operation.domain.OperationStatus.ERROR_CHECK;
import static fr.xelians.esafe.operation.domain.OperationStatus.ERROR_COMMIT;

import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.FunctionalException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CommitIndexTask extends AbstractOperationTask<Void> {

  protected CommitIndexTask(
      OperationDb operation, OperationService operationService, TenantService tenantService) {
    super(operation, operationService, tenantService);
    logEvent("CREATION");
  }

  @Override
  public Void call() {
    if (!isActive) {
      log.info(String.format("Operation '%s' is not active", operation.getId()));
      return null;
    }
    logEvent("INIT");
    // Check operation
    try {
      check();
      logEvent("CHECK");
    } catch (FunctionalException ex) {
      log.warn(format("Check operation failed", ex), ex);
      clean();
      operation.setStatus(ERROR_CHECK);
      operation.setOutcome(operation.getStatus().toString());
      operation.setTypeInfo(operation.getType().toString());
      operation.setMessage(
          String.format(
              "%s - Category: %s - Code: %s", ex.getTexts(), ex.getCategory(), ex.getCode()));
      operationService.unlockAndSave(operation, operation.getStatus(), operation.getMessage());
      return null;

    } catch (Exception ex) {
      clean();
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Check operation failed", e), ex);
      operation.setStatus(ERROR_CHECK);
      operation.setOutcome(operation.getStatus().toString());
      operation.setTypeInfo(operation.getType().toString());
      operation.setMessage(String.format("Error in check phase - Code: %s", e.getCode()));
      operationService.unlockAndSave(operation, operation.getStatus(), operation.getMessage());
      return null;
    }

    // Commit Operation
    try {
      commit();
      logEvent("COMMIT");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Commit operation failed", e), ex);
      operation.setStatus(ERROR_COMMIT);
      operation.setOutcome(operation.getStatus().toString());
      operation.setTypeInfo(operation.getType().toString());
      operation.setMessage(String.format("Error in commit phase - Code: %s", e.getCode()));
      operationService.unlockAndSave(operation, operation.getStatus(), operation.getMessage());
      return null;
    } finally {
      clean();
    }

    // Index operation
    try {
      index();
      logEvent("INDEX");
      operationService.unlockAndSave(operation, operation.getStatus(), operation.getMessage());
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Index operation failed", e), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage(String.format("Failed to index - retrying - Code: %s", e.getCode()));
      operationService.save(operation);
    }
    return null;
  }

  public abstract void check();

  public abstract void commit();

  // The index method must be idempotent
  public abstract void index();

  @Override
  public String toString() {
    return "IndexOperationTask{" + "operation=" + operation.getId() + '}';
  }
}
