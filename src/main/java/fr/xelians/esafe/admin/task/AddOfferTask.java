/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.OfferAdminService;
import fr.xelians.esafe.common.task.CheckTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddOfferTask extends CheckTask {

  private final OfferAdminService offerAdminService;

  public AddOfferTask(OperationDb operation, OfferAdminService offerAdminService) {
    super(operation, offerAdminService.getOperationService(), offerAdminService.getTenantService());
    this.offerAdminService = offerAdminService;
  }

  @Override
  public void check() {
    offerAdminService.check(operation);
  }

  @Override
  public String toString() {
    return "AddOfferTask{" + "operation=" + operation.getId() + '}';
  }
}
