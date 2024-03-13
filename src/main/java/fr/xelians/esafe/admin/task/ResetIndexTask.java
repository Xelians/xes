/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.IndexAdminService;
import fr.xelians.esafe.common.task.RunTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResetIndexTask extends RunTask {

  private final IndexAdminService indexAdminService;

  public ResetIndexTask(OperationDb operation, IndexAdminService indexAdminService) {
    super(operation, indexAdminService.getOperationService(), indexAdminService.getTenantService());
    this.indexAdminService = indexAdminService;
  }

  @Override
  public void run() {
    indexAdminService.check(operation);
  }

  @Override
  public String toString() {
    return "ResetIndexTask{" + "operation=" + operation.getId() + '}';
  }
}
