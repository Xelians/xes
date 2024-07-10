/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer;

import fr.xelians.esafe.common.constant.Logbook;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.storage.domain.StorageType;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public abstract class AbstractStorageOffer implements StorageOffer {

  protected final String name;
  protected final boolean isActive;
  protected final StorageType storageType;

  protected AbstractStorageOffer(String name, boolean isActive, StorageType storageType) {
    this.name = name;
    this.isActive = isActive;
    this.storageType = storageType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isActive() {
    return isActive;
  }

  @Override
  public StorageType getStorageType() {
    return storageType;
  }

  public boolean isEncrypted() {
    return false;
  }

  protected static void writeLbkOps(Writer logWriter, List<OperationDb> operations)
      throws IOException {
    for (OperationDb op : operations) {
      String message = op.getType() == OperationType.EXTERNAL ? op.getMessage() : "";

      logWriter.write(
          "BEGIN;"
              + op.getType()
              + Logbook.LINE_SEP
              + op.getTenant()
              + Logbook.LINE_SEP
              + op.getId()
              + Logbook.LINE_SEP
              + op.getUserIdentifier()
              + Logbook.LINE_SEP
              + op.getApplicationId()
              + Logbook.LINE_SEP
              + op.getTypeInfo()
              + Logbook.LINE_SEP
              + op.getOutcome()
              + Logbook.LINE_SEP
              + message
              + Logbook.LINE_SEP
              + op.getObjectIdentifier()
              + Logbook.LINE_SEP
              + op.getObjectInfo()
              + Logbook.LINE_SEP
              + op.getObjectData()
              + Logbook.LINE_SEP
              + op.getCreated()
              + Logbook.END_LINE);

      for (String action : op.getActions()) {
        logWriter.write(action + Logbook.END_LINE);
      }

      logWriter.write(
          "COMMIT;"
              + op.getType()
              + Logbook.LINE_SEP
              + op.getTenant()
              + Logbook.LINE_SEP
              + op.getId()
              + Logbook.LINE_SEP
              + op.getModified()
              + Logbook.END_LINE);
    }
  }

  protected static <T extends Future<?>> void waitFutures(List<T> futures) throws IOException {
    try {
      for (Future<?> future : futures) {
        future.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new IOException(e);
    }
  }
}
