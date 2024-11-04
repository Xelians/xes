/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.capacity;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public class CapacityLbkIterator extends AllLbkIterator {

  public CapacityLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(LogbookOperation logbookOperation, String[] tokens) {
    logbookOperation.addAction(StorageAction.create(tokens));
  }

  @Override
  public void actionUpdate(LogbookOperation logbookOperation, String[] tokens) {
    // We only need to know the max capacity so processing update is not necessary
  }

  @Override
  public void actionDelete(LogbookOperation logbookOperation, String[] tokens) {
    // We only need to know the max capacity hence we should not process delete
  }
}
