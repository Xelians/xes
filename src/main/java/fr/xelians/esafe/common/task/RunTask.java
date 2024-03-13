/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import static fr.xelians.esafe.operation.domain.OperationStatus.*;

import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.FunctionalException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class RunTask extends AbstractOperationTask<Void> {

  protected RunTask(
      OperationDb operation, OperationService operationService, TenantService tenantService) {
    super(operation, operationService, tenantService);
  }

  @Override
  public Void call() {
    if (!isActive) {
      log.info(String.format("Operation '%s' is not active", operation.getId()));
      return null;
    }

    // Check operation
    try {
      run();
    } catch (FunctionalException ex) {
      clean();
      operationService.unlockAndSave(
          operation,
          ERROR_CHECK,
          String.format(
              "%s - Category: %s - Code: %s", ex.getTexts(), ex.getCategory(), ex.getCode()));
      log.warn(format("Check operation failed", ex), ex);
      return null;
    } catch (Exception ex) {
      clean();
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      operationService.unlockAndSave(
          operation, ERROR_CHECK, String.format("Error in check phase - Code: %s", e.getCode()));
      log.error(format("Check operation failed", e), ex);
      return null;
    }

    return null;
  }

  public abstract void run();

  @Override
  public String toString() {
    return "CommitOperationTask{operation=" + operation.getId() + '}';
  }
}
