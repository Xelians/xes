/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.domain.elimination.Eliminator;
import fr.xelians.esafe.archive.service.EliminationService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;

// Exclusive task
/*
 * @author Emmanuel Deviller
 */
public class EliminationTask extends StoreIndexTask {

  private final EliminationService eliminationService;
  private final TenantDb tenantDb;
  private Eliminator eliminator;

  public EliminationTask(OperationDb operation, EliminationService eliminationService) {

    super(
        operation, eliminationService.getOperationService(), eliminationService.getTenantService());
    this.eliminationService = eliminationService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    eliminator = eliminationService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    eliminationService.commit(operation, tenantDb, eliminator);
  }

  @Override
  public void store() {
    eliminationService.store(operation, tenantDb, eliminator);
  }

  @Override
  public void index() {
    eliminationService.index(operation);
  }
}
