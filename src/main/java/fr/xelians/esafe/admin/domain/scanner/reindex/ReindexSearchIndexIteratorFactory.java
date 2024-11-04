/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.reindex;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.unit.UnitDbIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.unit.UnitLbkIterator;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record ReindexSearchIndexIteratorFactory(
    StorageService storageService, OperationService operationService) implements IteratorFactory {

  @Override
  public LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers) {
    return new UnitLbkIterator(tenantDb, offers, storageService);
  }

  @Override
  public DbIterator createDbIterator(Long tenant, long maxOperationId) {
    return new UnitDbIterator(tenant, maxOperationId, operationService);
  }
}
