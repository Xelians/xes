/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.ReclassificationService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

// Exclusive task
public class ReclassificationTask extends StoreIndexTask {

  private final ReclassificationService reclassificationService;
  private final TenantDb tenantDb;
  private Path tmpAusPath;

  public ReclassificationTask(
      OperationDb operation, ReclassificationService reclassificationService) {

    super(
        operation,
        reclassificationService.getOperationService(),
        reclassificationService.getTenantService());
    this.reclassificationService = reclassificationService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    tmpAusPath = reclassificationService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    reclassificationService.commit(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void store() {
    reclassificationService.store(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void index() {
    reclassificationService.index(operation);
  }
}
