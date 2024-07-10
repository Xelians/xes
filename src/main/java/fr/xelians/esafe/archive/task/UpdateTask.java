/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.UpdateService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

// Exclusive task
public class UpdateTask extends StoreIndexTask {

  private final UpdateService updateService;
  private final TenantDb tenantDb;
  private Path tmpAusPath;

  public UpdateTask(OperationDb operation, UpdateService updateService) {
    super(operation, updateService.getOperationService(), updateService.getTenantService());
    this.updateService = updateService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    tmpAusPath = updateService.check(operation, tenantDb);
  }

  // Commit SIP
  @Override
  public void commit() {
    updateService.commit(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void store() {
    updateService.store(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void index() {
    updateService.index(operation);
  }
}
