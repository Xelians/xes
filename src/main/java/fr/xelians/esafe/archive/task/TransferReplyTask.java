/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.domain.elimination.Eliminator;
import fr.xelians.esafe.archive.service.TransferReplyService;
import fr.xelians.esafe.common.task.StoreIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;

// Exclusive task
/*
 * @author Emmanuel Deviller
 */
public class TransferReplyTask extends StoreIndexTask {

  private final TransferReplyService transferReplyService;
  private final TenantDb tenantDb;
  private Eliminator eliminator;

  public TransferReplyTask(OperationDb operation, TransferReplyService transferReplyService) {
    super(
        operation,
        transferReplyService.getOperationService(),
        transferReplyService.getTenantService());
    this.transferReplyService = transferReplyService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    eliminator = transferReplyService.check(operation, tenantDb);
  }

  @Override
  public void commit() {
    transferReplyService.commit(operation, tenantDb, eliminator);
  }

  @Override
  public void store() {
    transferReplyService.store(operation, tenantDb, eliminator);
  }

  @Override
  public void index() {
    transferReplyService.index(operation);
  }
}
