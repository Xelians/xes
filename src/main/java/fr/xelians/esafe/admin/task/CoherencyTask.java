/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.task;

import fr.xelians.esafe.admin.service.CoherencyService;
import fr.xelians.esafe.common.task.CheckTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class CoherencyTask extends CheckTask {

  private final CoherencyService coherencyService;

  public CoherencyTask(OperationDb operation, CoherencyService coherencyService) {
    super(operation, coherencyService.getOperationService(), coherencyService.getTenantService());
    this.coherencyService = coherencyService;
  }

  public void check() {
    coherencyService.check(operation);
  }

  @Override
  public String toString() {
    return "CoherencyTask{" + "operation=" + operation.getId() + '}';
  }
}
