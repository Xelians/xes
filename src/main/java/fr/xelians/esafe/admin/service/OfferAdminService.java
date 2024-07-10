/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.service;

import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.admin.domain.scanner.addoffer.AddOfferIteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.addoffer.AddOfferProcessor;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import fr.xelians.esafe.common.utils.OperationUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.hashset.StorageObjectSet;
import fr.xelians.esafe.storage.domain.object.HashStorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class OfferAdminService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";

  // We use a custom thread pool to avoid blocking the default one
  private static final ForkJoinPool STORAGE_POOL = new ForkJoinPool(Utils.CPUS * 2);
  public static final String ADD_STORAGE_OFFER_FAILED = "Add storage offer failed";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final AdminService adminService;

  @Transactional
  public void removeOffer(Long tenant, String offer) {
    TenantDb tenantDb = tenantService.getTenantDbForUpdate(tenant);
    List<String> offers = tenantDb.getStorageOffers();
    if (!offers.contains(offer)) {
      throw new BadRequestException(
          "Remove storage offer failed",
          String.format("Failed to remove not existing offer '%s' in tenant %s", offer, tenant));
    }
    offers.remove(offer);
  }

  // Don't create and submit a task in a transaction
  @Transactional(rollbackFor = Exception.class)
  public OperationDb addOffer(Long tenant, String dstOffer, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    if (!storageService.existsStorageOffer(dstOffer)) {
      throw new BadRequestException(
          ADD_STORAGE_OFFER_FAILED, String.format("Storage offer %s does not exist", dstOffer));
    }

    // Get and lock tenant during the transaction
    TenantDb tenantDb = tenantService.getTenantDbForUpdate(tenant);
    List<String> srcOffers = tenantDb.getStorageOffers();

    if (srcOffers.contains(dstOffer)) {
      throw new BadRequestException(
          ADD_STORAGE_OFFER_FAILED,
          String.format("Storage offer %s already exists in tenant %s", dstOffer, tenant));
    }

    if (srcOffers.size() > 2) {
      throw new BadRequestException(
          ADD_STORAGE_OFFER_FAILED, String.format("Too many offers in tenant %s", tenant));
    }

    // Run only one operation
    List<OperationDb> operations =
        operationService.findByTenantAndTypeAndStatus(
            tenant, OperationType.ADD_OFFER, OperationStatus.RUN);
    if (!operations.isEmpty()) {
      throw new BadRequestException(
          ADD_STORAGE_OFFER_FAILED,
          String.format(
              "Storage offer operation with id %d is already running",
              operations.getFirst().getId()));
    }

    // Create operation to copy existing offers to this new offer
    OperationDb operation = OperationFactory.addOfferOp(tenant, user, app, dstOffer);

    // Add offer to tenant
    srcOffers.add(dstOffer);

    operation.setStatus(OperationStatus.INIT);
    operation.setMessage("Add storage offer");
    return operationService.save(operation);
  }

  public void check(OperationDb operation) {
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    String dstOffer = operation.getProperty01();

    // Warning: the task most be created outside the transaction to avoid removing dstOffer from
    // storageOffers
    List<String> srcOffers = tenantDb.getStorageOffers();
    srcOffers.remove(dstOffer);

    try {
      // Wait for working operations that started before this task to finish
      operationService.waitForSecureOperationsToBeStored(tenant);
      operationService.waitForArchiveOperationsToBeStored(tenant);

      // Copy logbooks and archives
      copyLogbooks(tenantDb, srcOffers, dstOffer);
      copyArchives(tenantDb, srcOffers, dstOffer);
      // TODO copy refrentials, backup and ope

      operationService.unlockAndSave(
          operation,
          OperationStatus.OK,
          String.format("Add storage offer '%s' completed with success", dstOffer));
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);

      String msg =
          ExceptionsUtils.format(
              String.format("Failed to add storage offer '%s'", dstOffer), e, operation);
      log.error(msg, ex);
      operationService.unlockAndSave(
          operation,
          OperationStatus.FATAL,
          String.format("Failed to add storage offer '%s' - Code: %s", dstOffer, e.getCode()));
    }
  }

  // Copy all completed storage logs to the new offer
  private void copyLogbooks(TenantDb tenantDb, List<String> srcOffers, String dstOffer)
      throws IOException {

    Long tenant = tenantDb.getId();
    // Get the last secure number
    long maxSecureNumber = storageService.getSecureNumber(tenant);
    List<Long> ids =
        storageService.findLbkIds(tenantDb, srcOffers).stream()
            .filter(n -> n <= maxSecureNumber)
            .toList();
    for (Long id : ids) {
      storageService.copy(tenant, srcOffers, id, StorageObjectType.lbk, dstOffer);
    }
  }

  private void copyArchives(TenantDb tenantDb, List<String> srcOffers, String dstOffer)
      throws IOException, ExecutionException, InterruptedException {

    Long tenant = tenantDb.getId();
    // Try to restrict copy to the minimum
    long operationId = operationService.findLastArchiveOperationIdToProcess(tenant);
    long capacity = adminService.getActionsSize(tenantDb, srcOffers, operationId);
    log.info("Tenant {} - Capacity {}", tenant, capacity);

    if (capacity > 0) {
      try (StorageObjectSet storageObjectSet = OperationUtils.createStorageObjectSet(capacity)) {
        IteratorFactory factory = new AddOfferIteratorFactory(storageService, operationService);
        OperationProcessor processor = new AddOfferProcessor(storageObjectSet);
        OperationUtils.scanDb(operationId, tenant, factory, processor);
        OperationUtils.scanLbk(tenantDb, srcOffers, factory, processor);

        // Chronicle Map Iterator is not thread safe and must only be accessed from a single thread
        // So we copy chunk of map data into a list before processing
        for (Iterator<List<HashStorageObject>> it = storageObjectSet.listIterator();
            it.hasNext(); ) {
          List<HashStorageObject> hashStorageObjects = it.next();
          STORAGE_POOL.submit(copy(tenant, srcOffers, dstOffer, hashStorageObjects)).get();
        }
      }
    }
  }

  private Runnable copy(
      Long tenant, List<String> srcOffers, String dstOffer, List<HashStorageObject> sos) {
    return () -> sos.parallelStream().forEach(so -> copy(tenant, srcOffers, dstOffer, so));
  }

  @SneakyThrows
  private void copy(Long tenant, List<String> srcOffers, String dstOffer, HashStorageObject so) {
    storageService.copy(tenant, srcOffers, so.getId(), so.getType(), dstOffer);
  }
}
