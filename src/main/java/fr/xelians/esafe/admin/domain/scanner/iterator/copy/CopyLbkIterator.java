/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.copy;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class CopyLbkIterator extends AllLbkIterator {

  public CopyLbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(LogbookOperation logbookOperation, String[] tokens) {
    // TODO Optimise by removing tokens 3 and 4 that are not necessary
    if (logbookOperation.getType() == OperationType.INGEST_ARCHIVE
        || logbookOperation.getType() == OperationType.INGEST_HOLDING
        || logbookOperation.getType() == OperationType.INGEST_FILING) {

      logbookOperation.addAction(StorageAction.create(tokens));
    }
  }

  @Override
  public void actionUpdate(LogbookOperation logbookOperation, String[] tokens) {
    // We only need to know existing archives (i.e. created and not yet deleted archive) so update
    // is not necessary
  }

  @Override
  public void actionDelete(LogbookOperation logbookOperation, String[] tokens) {
    if (logbookOperation.getType() == OperationType.ELIMINATE_ARCHIVE) {
      logbookOperation.addAction(StorageAction.create(tokens));
    }
  }
}
