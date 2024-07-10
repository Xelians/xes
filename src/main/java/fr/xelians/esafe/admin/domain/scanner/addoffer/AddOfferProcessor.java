/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.addoffer;

import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.storage.domain.hashset.StorageObjectSet;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddOfferProcessor implements OperationProcessor {

  private final StorageObjectSet storageObjectSet;

  public AddOfferProcessor(StorageObjectSet storageObjectSet) {
    this.storageObjectSet = storageObjectSet;
  }

  @Override
  public void process(LogbookOperation operation) {
    OperationType type = operation.getType();
    if (type == OperationType.INGEST_ARCHIVE
        || type == OperationType.INGEST_HOLDING
        || type == OperationType.INGEST_FILING) {

      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getActionType() == ActionType.CREATE) {
          storageObjectSet.add(
              storageAction.getId(),
              storageAction.getType(),
              storageAction.getHash(),
              storageAction.getChecksum());
        }
      }
    } else if (type == OperationType.ELIMINATE_ARCHIVE) {
      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getActionType() == ActionType.DELETE) {
          storageObjectSet.remove(storageAction.getId(), storageAction.getType());
        }
      }
    }
  }

  @Override
  public void finish() {
    // Nothing to do here
  }
}
