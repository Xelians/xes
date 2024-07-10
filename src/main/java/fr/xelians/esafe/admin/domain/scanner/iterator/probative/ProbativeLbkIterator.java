/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.probative;

import fr.xelians.esafe.admin.domain.scanner.OneLbkIterator;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class ProbativeLbkIterator extends OneLbkIterator {

  public ProbativeLbkIterator(TenantDb tenantDb, StorageService storageService, long logId) {
    super(tenantDb, tenantDb.getStorageOffers(), storageService, logId);
  }

  public ProbativeLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService, long logId) {
    super(tenantDb, offers, storageService, logId);
  }

  @Override
  public void actionCreate(LogbookOperation logbookOperation, String[] tokens) {
    if (logbookOperation.getType() == OperationType.INGEST_ARCHIVE) {
      StorageAction storageAction = StorageAction.create(tokens);
      if (storageAction.getType() == StorageObjectType.atr) {
        logbookOperation.addAction(storageAction);
      }
    }
  }

  @Override
  public void actionUpdate(LogbookOperation logbookOperation, String[] tokens) {
    // TODO Deal with update binary version
  }

  @Override
  public void actionDelete(LogbookOperation logbookOperation, String[] tokens) {}
}
