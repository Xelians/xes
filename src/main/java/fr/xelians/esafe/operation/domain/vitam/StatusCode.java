/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain.vitam;

import fr.xelians.esafe.operation.domain.OperationStatus;

public enum StatusCode {
  UNKNOWN,
  STARTED,
  ALREADY_EXECUTED,
  OK,
  WARNING,
  KO,
  FATAL;

  public static StatusCode from(OperationStatus operationStatus) {
    return switch (operationStatus) {
      case INIT -> UNKNOWN;
      case ERROR_INIT, ERROR_CHECK, ERROR_COMMIT -> KO;
      case BACKUP, RUN, INDEX, STORE, RETRY_STORE, RETRY_INDEX -> STARTED;
      case OK -> OK;
      case FATAL -> FATAL;
    };
  }
}
