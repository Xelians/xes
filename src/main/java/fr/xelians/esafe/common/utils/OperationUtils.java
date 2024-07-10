/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.hashset.*;
import java.io.IOException;
import java.util.List;

public final class OperationUtils {

  private OperationUtils() {}

  public static OperationType getIngestOperationType(ContextId contextId) {
    return switch (contextId) {
      case DEFAULT_WORKFLOW -> OperationType.INGEST_ARCHIVE;
      case FILING_SCHEME -> OperationType.INGEST_FILING;
      case HOLDING_SCHEME -> OperationType.INGEST_HOLDING;
    };
  }

  public static IdSet createIdSet(long capacity) throws IOException {
    // TODO need configuration
    return capacity < 200_000 ? new HashIdSet((int) capacity) : new ChronicleIdSet(capacity);
  }

  public static StorageObjectSet createStorageObjectSet(long capacity) throws IOException {
    // TODO need configuration
    return capacity < 100_000
        ? new HashStorageObjectSet((int) capacity)
        : new ChronicleStorageObjectSet(capacity);
  }

  public static void scanLbk(
      TenantDb tenantDb, List<String> offers, IteratorFactory factory, OperationProcessor processor)
      throws IOException {

    try (LbkIterator it = factory.createLbkIterator(tenantDb, offers)) {
      while (it.hasNext()) {
        processor.process(it.next());
      }
      processor.finish();
    }
  }

  public static void scanDb(
      Long maxOperationId, Long tenant, IteratorFactory factory, OperationProcessor processor)
      throws IOException {

    if (maxOperationId >= 0) {
      DbIterator it = factory.createDbIterator(tenant, maxOperationId);
      while (it.hasNext()) {
        processor.process(it.next().toOperationSe());
      }
      processor.finish();
    }
  }
}
