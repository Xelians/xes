/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.CoherencyService;
import fr.xelians.esafe.common.task.RunTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoherencyTask extends RunTask {

  private final CoherencyService coherencyService;

  public CoherencyTask(OperationDb operation, CoherencyService coherencyService) {
    super(operation, coherencyService.getOperationService(), coherencyService.getTenantService());
    this.coherencyService = coherencyService;
  }

  @Override
  public void run() {
    coherencyService.check(operation);
  }

  @Override
  public String toString() {
    return "CoherencyTask{" + "operation=" + operation.getId() + '}';
  }
}
