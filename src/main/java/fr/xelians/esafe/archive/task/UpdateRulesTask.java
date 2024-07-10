/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.UpdateRulesService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import java.nio.file.Path;

// Exclusive task
public class UpdateRulesTask extends StoreIndexTask {

  private final UpdateRulesService updateRulesService;
  private final TenantDb tenantDb;
  private Path tmpAusPath;

  public UpdateRulesTask(OperationDb operation, UpdateRulesService updateRulesService) {
    super(
        operation, updateRulesService.getOperationService(), updateRulesService.getTenantService());
    this.updateRulesService = updateRulesService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    tmpAusPath = updateRulesService.check(operation, tenantDb);
  }

  // Commit SIP
  @Override
  public void commit() {
    updateRulesService.commit(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void store() {
    updateRulesService.store(operation, tenantDb, tmpAusPath);
  }

  @Override
  public void index() {
    updateRulesService.index(operation);
  }
}
