/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.ExportService;
import fr.xelians.esafe.common.task.CommitTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

public class ExportTask extends CommitTask {

  private final TenantDb tenantDb;
  private final ExportService exportService;
  private Path tmpDipPath;

  public ExportTask(OperationDb operation, ExportService exportService) {
    super(operation, exportService.getOperationService(), exportService.getTenantService());
    this.exportService = exportService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    tmpDipPath = exportService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    exportService.commit(operation, tenantDb, tmpDipPath);
  }
}
