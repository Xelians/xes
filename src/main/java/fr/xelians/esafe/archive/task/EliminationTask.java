/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.EliminationService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

// Exclusive task
public class EliminationTask extends StoreIndexTask {

  private final EliminationService eliminationService;
  private final TenantDb tenantDb;
  private Path tmpAusPath;

  public EliminationTask(OperationDb operation, EliminationService eliminationService) {

    super(
        operation, eliminationService.getOperationService(), eliminationService.getTenantService());
    this.eliminationService = eliminationService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    tmpAusPath = eliminationService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    eliminationService.commit(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void store() {
    eliminationService.store(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void index() {
    eliminationService.index(operation);
  }
}
