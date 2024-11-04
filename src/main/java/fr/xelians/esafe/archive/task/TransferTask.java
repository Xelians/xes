/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.service.TransferService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;

// Exclusive task
/*
 * @author Emmanuel Deviller
 */
public class TransferTask extends StoreIndexTask {

  private final TransferService transferService;
  private final TenantDb tenantDb;
  private TransferService.TransferPaths paths;

  public TransferTask(OperationDb operation, TransferService transferService) {
    super(operation, transferService.getOperationService(), transferService.getTenantService());
    this.transferService = transferService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    paths = transferService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    transferService.commit(operation, tenantDb, paths);
  }

  @Override
  public void store() {
    transferService.store(operation, tenantDb, paths);
  }

  @Override
  public void index() {
    transferService.index(operation);
  }
}
