/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.IndexAdminService;
import fr.xelians.esafe.common.task.CheckTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResetTenantIndexTask extends CheckTask {

  private final IndexAdminService indexAdminService;

  public ResetTenantIndexTask(OperationDb operation, IndexAdminService indexAdminService) {
    super(operation, indexAdminService.getOperationService(), indexAdminService.getTenantService());
    this.indexAdminService = indexAdminService;
  }

  public void check() {
    indexAdminService.checkTenant(operation);
  }

  @Override
  public String toString() {
    return "ResetTenantIndexTask{" + "operation=" + operation.getId() + '}';
  }
}
