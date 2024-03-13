/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.OfferAdminService;
import fr.xelians.esafe.common.task.RunTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddOfferTask extends RunTask {

  private final OfferAdminService offerAdminService;

  public AddOfferTask(OperationDb operation, OfferAdminService offerAdminService) {
    super(operation, offerAdminService.getOperationService(), offerAdminService.getTenantService());
    this.offerAdminService = offerAdminService;
  }

  @Override
  public void run() {
    offerAdminService.check(operation);
  }

  @Override
  public String toString() {
    return "AddOfferTask{" + "operation=" + operation.getId() + '}';
  }
}
