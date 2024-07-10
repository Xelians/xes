/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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
