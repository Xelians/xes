/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner;

import fr.xelians.esafe.organization.entity.TenantDb;
import java.util.List;

public interface IteratorFactory {

  LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers);

  DbIterator createDbIterator(Long tenant, long maxOperationId);
}
