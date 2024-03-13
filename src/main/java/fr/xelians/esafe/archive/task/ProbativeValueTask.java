/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.ProbativeValueService;
import fr.xelians.esafe.common.task.CommitTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

public class ProbativeValueTask extends CommitTask {

  private final TenantDb tenantDb;
  private final ProbativeValueService probativeValueService;
  private Path reportPath;

  public ProbativeValueTask(OperationDb operation, ProbativeValueService probativeValueService) {
    super(
        operation,
        probativeValueService.getOperationService(),
        probativeValueService.getTenantService());
    this.probativeValueService = probativeValueService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    reportPath = probativeValueService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    probativeValueService.commit(operation, tenantDb, reportPath);
  }
}
