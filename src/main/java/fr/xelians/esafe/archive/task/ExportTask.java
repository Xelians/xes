/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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
