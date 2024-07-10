/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.domain;

import fr.xelians.esafe.common.constant.Env;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Workspace {

  private Workspace() {
    // Do nothing
  }

  public static Path getPath(OperationDb operation) {
    return getPath(operation.getId());
  }

  public static Path getPath(Long operationId) {
    return Env.OPERATION_PATH.resolve(operationId.toString());
  }

  public static Path getPath(OperationDb operation, String suffix) {
    return getPath(operation.getId()).resolve(suffix);
  }

  public static Path createTempFile(OperationDb operation) throws IOException {
    Path ws = getPath(operation.getId());
    if (Files.notExists(ws)) Files.createDirectories(ws);
    return Files.createTempFile(ws, null, null);
  }
}
