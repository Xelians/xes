/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.securing;

import fr.xelians.esafe.admin.domain.scanner.OneLbkIterator;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public class SecuringLbkIterator extends OneLbkIterator {

  public SecuringLbkIterator(TenantDb tenantDb, StorageService storageService, long logId) {
    super(tenantDb, tenantDb.getStorageOffers(), storageService, logId);
  }

  public SecuringLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService, long logId) {
    super(tenantDb, offers, storageService, logId);
  }

  @Override
  public void actionCreate(LogbookOperation logbookOperation, String[] tokens) {
    // Nothing to do
  }

  @Override
  public void actionUpdate(LogbookOperation logbookOperation, String[] tokens) {
    // Nothing to do
  }

  @Override
  public void actionDelete(LogbookOperation logbookOperation, String[] tokens) {
    // Nothing to do
  }
}
