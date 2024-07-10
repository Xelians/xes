/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.unit;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class UnitLbkIterator extends AllLbkIterator {

  // Fetch all operations (needed for indexing operations)
  // Process only add/delete actions for unit type only (needed for indexing archive)
  // Keep only id (type is always unit) - needed for add/remove in StorageActionSet

  public UnitLbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(LogbookOperation logbookOperation, String[] tokens) {
    if (StorageObjectType.uni.name().equals(tokens[2])) {
      logbookOperation.addAction(StorageAction.create(tokens));
    }
  }

  @Override
  public void actionUpdate(LogbookOperation logbookOperation, String[] tokens) {
    // We only need to know existing archive units (i.e. created and not yet deleted archive unit)
    // so update is not necessary
  }

  @Override
  public void actionDelete(LogbookOperation logbookOperation, String[] tokens) {
    if (StorageObjectType.uni.name().equals(tokens[2])) {
      logbookOperation.addAction(StorageAction.create(tokens));
    }
  }
}
