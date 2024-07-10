/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.task;

import fr.xelians.esafe.operation.entity.OperationDb;

public interface OperationTask extends CleanableTask {

  OperationDb getOperation();

  void setActive(boolean b);

  boolean isActive();

  boolean isExclusive();
}
