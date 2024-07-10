/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner.coherency;

import fr.xelians.esafe.admin.domain.scanner.*;
import fr.xelians.esafe.admin.domain.scanner.iterator.check.CheckDbIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.check.CheckLbkIterator;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public record CoherencyIteratorFactory(
    StorageService storageService, OperationService operationService) implements IteratorFactory {

  @Override
  public LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers) {
    return new CheckLbkIterator(tenantDb, offers, storageService);
  }

  @Override
  public DbIterator createDbIterator(Long tenant, long maxOperationId) {
    return new CheckDbIterator(tenant, maxOperationId, operationService);
  }
}
