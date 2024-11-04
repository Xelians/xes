/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.task;

import java.util.concurrent.FutureTask;
import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public class FutureOperationTask<T> extends FutureTask<T> {

  private final OperationTask operationTask;

  public FutureOperationTask(OperationTask callable) {
    super(callable, null);
    this.operationTask = callable;
  }
}
