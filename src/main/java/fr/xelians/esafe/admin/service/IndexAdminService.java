/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.service;

import static fr.xelians.esafe.operation.domain.OperationFactory.rebuildIndexOp;
import static fr.xelians.esafe.operation.domain.OperationFactory.resetTenantIndexOp;

import fr.xelians.esafe.accession.domain.search.RegisterDetailsIndex;
import fr.xelians.esafe.accession.domain.search.RegisterIndex;
import fr.xelians.esafe.accession.domain.search.RegisterSummaryIndex;
import fr.xelians.esafe.accession.domain.search.RegisterSymbolicIndex;
import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.admin.domain.scanner.iterator.capacity.CapacityDbIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.capacity.CapacityLbkIterator;
import fr.xelians.esafe.admin.domain.scanner.reindex.ReindexSearchIndexIteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.reindex.ReindexSearchIndexProcessor;
import fr.xelians.esafe.admin.task.ResetIndexTask;
import fr.xelians.esafe.admin.task.ResetTenantIndexTask;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.service.IndexService;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import fr.xelians.esafe.common.utils.OperationUtils;
import fr.xelians.esafe.logbook.domain.search.LogbookIndex;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.hashset.IdSet;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class IndexAdminService {

  public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
  public static final String FAILED_TO_RESET_INDEX = "Failed to reset search engine index";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final SearchEngineService searchEngineService;
  private final LogbookService logbookService;
  private final IndexService indexService;

  @Value("${app.indexing.threads:16}")
  private int indexingThreads;

  public void createAllIndex() throws IOException {
    createIndex(ArchiveUnitIndex.NAME, ArchiveUnitIndex.ALIAS, ArchiveUnitIndex.MAPPING);
    createIndex(LogbookIndex.NAME, LogbookIndex.ALIAS, LogbookIndex.MAPPING);
    createIndex(
        RegisterDetailsIndex.NAME, RegisterDetailsIndex.ALIAS, RegisterDetailsIndex.MAPPING);
    createIndex(RegisterSummaryIndex.NAME, RegisterSummaryIndex.ALIAS, RegisterIndex.MAPPING);
    createIndex(RegisterSymbolicIndex.NAME, RegisterSymbolicIndex.ALIAS, RegisterIndex.MAPPING);
  }

  // Delete archive unit and logbook index
  // TODO Check if a rebuild operation is already running  (Status = commit)
  public void deleteAllIndex() throws IOException {
    searchEngineService.deleteIndexWithPrefix(RegisterSymbolicIndex.NAME);
    searchEngineService.deleteIndexWithPrefix(RegisterSummaryIndex.NAME);
    searchEngineService.deleteIndexWithPrefix(RegisterDetailsIndex.NAME);
    searchEngineService.deleteIndexWithPrefix(LogbookIndex.NAME);
    searchEngineService.deleteIndexWithPrefix(ArchiveUnitIndex.NAME);
  }

  // TODO Check if a rebuild operation is already running  (Status = commit)
  public void resetAllIndex() throws IOException {
    resetIndex(RegisterSymbolicIndex.NAME, RegisterSymbolicIndex.ALIAS, RegisterIndex.MAPPING);
    resetIndex(RegisterSummaryIndex.NAME, RegisterSummaryIndex.ALIAS, RegisterIndex.MAPPING);
    resetIndex(RegisterDetailsIndex.NAME, RegisterDetailsIndex.ALIAS, RegisterDetailsIndex.MAPPING);
    resetIndex(LogbookIndex.NAME, LogbookIndex.ALIAS, LogbookIndex.MAPPING);
    resetIndex(ArchiveUnitIndex.NAME, ArchiveUnitIndex.ALIAS, ArchiveUnitIndex.MAPPING);
  }

  private void createIndex(final String name, final String alias, final String mapping)
      throws IOException {
    Set<String> indices;
    indices = searchEngineService.getIndicesByAlias(alias);
    if (CollectionUtils.isEmpty(indices)) {
      searchEngineService.createIndex(buildIndexName(name), alias, mapping);
    }
  }

  private void resetIndex(final String name, final String alias, final String mapping)
      throws IOException {
    searchEngineService.removeAlias(alias);
    searchEngineService.deleteIndex(alias);
    searchEngineService.deleteIndexWithPrefix(name);
    searchEngineService.createIndex(buildIndexName(name), alias, mapping);
  }

  private String buildIndexName(final String indexName) {
    return indexName + "." + LocalDateTime.now().format(FMT);
  }

  // Delete, create and update archive unit and logbook index
  public Long rebuildIndex(Long tenant, String user, String app) throws IOException {
    resetAllIndex();
    OperationDb ope = operationService.save(rebuildIndexOp(tenant, user, app));
    processingService.submit(new ResetIndexTask(ope, this));
    return ope.getId();
  }

  // Delete, create and update archive unit and logbook index
  public Long updateIndex(Long tenant, String user, String app) {
    OperationDb ope = operationService.save(rebuildIndexOp(tenant, user, app));
    processingService.submit(new ResetIndexTask(ope, this));
    return ope.getId();
  }

  // Check for all tenants
  public void check(OperationDb operation) {
    String user = operation.getUserIdentifier();
    String app = operation.getApplicationId();

    try {
      List<Long> opeIds = new ArrayList<>();
      for (TenantDb tenantDb : tenantService.getTenantsDb()) {
        Long tenant = tenantDb.getId();
        log.info("Tenant: " + tenant);
        OperationDb ope = operationService.save(resetTenantIndexOp(tenant, user, app));
        processingService.submit(new ResetTenantIndexTask(ope, this));
        opeIds.add(ope.getId());
        if (opeIds.size() > indexingThreads) {
          log.info("waitOperation: " + tenant);
          opeIds.forEach(this::waitOperation);
          log.info("waitedOperation: " + tenant);
          opeIds.clear();
        }
      }
      opeIds.forEach(this::waitOperation);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Rebuild index completed with success");

    } catch (Exception ex) {
      // TODO Cancel/delete all remaining operationIds
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      String msg = ExceptionsUtils.format(FAILED_TO_RESET_INDEX, e, operation);
      log.error(msg, ex);
      operationService.unlockAndSave(
          operation,
          OperationStatus.FATAL,
          String.format("Failed to reset search engine index - Code: %s", e.getCode()));
    }
  }

  public void checkTenant(OperationDb operation) {

    try {
      reindexSearchIndex(operation);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Reset index done with success");

    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      String msg = ExceptionsUtils.format(FAILED_TO_RESET_INDEX, e, operation);
      log.error(msg, ex);
      operationService.unlockAndSave(
          operation,
          OperationStatus.FATAL,
          String.format("Failed to reset index - Code: %s", e.getCode()));
    }
  }

  private void reindexSearchIndex(OperationDb operation)
      throws IOException, ExecutionException, InterruptedException {

    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();
    Long maxOperationId = operation.getId();

    log.info("tenant {} - getOperationsLength {}", tenant, maxOperationId);
    long capacity = getOperationsSize(tenantDb, offers, maxOperationId);

    if (capacity > 0) {
      // Create the set that will contain all id
      try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
          IdSet idSet = OperationUtils.createIdSet(capacity)) {
        IteratorFactory factory =
            new ReindexSearchIndexIteratorFactory(storageService, operationService);
        OperationProcessor processor = new ReindexSearchIndexProcessor(idSet, logbookService);
        OperationUtils.scanDb(maxOperationId, tenant, factory, processor);
        OperationUtils.scanLbk(tenantDb, offers, factory, processor);
        // Chronicle Map Iterator is not thread safe and must only be accessed from a single thread
        for (Iterator<List<Long>> it = idSet.listIterator(); it.hasNext(); ) {
          indexService.indexArchives(storageDao, tenant, offers, it.next());
        }
      }
    }
  }

  private void waitOperation(Long operationId) {
    OperationStatus status =
        operationService.waitOperation(
            operationId, 900_000, OperationStatus.OK, OperationStatus.FATAL);
    if (status != OperationStatus.OK) {
      throw new InternalException(
          "Wait for operation failed",
          String.format("Failed operation status '%s - status: %s'", operationId, status));
    }
    operationService.deleteOperations(operationId);
  }

  public long getOperationsSize(TenantDb tenantDb, List<String> offers, Long maxOperationId)
      throws IOException {

    long size = 0;
    try (LbkIterator scanner = new CapacityLbkIterator(tenantDb, offers, storageService)) {
      while (scanner.hasNext()) {
        scanner.next();
        size++;
      }
    }

    if (maxOperationId >= 0) {
      DbIterator scanner =
          new CapacityDbIterator(tenantDb.getId(), maxOperationId, operationService);
      while (scanner.hasNext()) {
        scanner.next();
        size++;
      }
    }
    return size;
  }
}
