/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.service;

import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.admin.domain.scanner.coherency.CoherencyIteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.coherency.CoherencyProcessor;
import fr.xelians.esafe.admin.task.CoherencyTask;
import fr.xelians.esafe.admin.task.CoherencyTenantTask;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class CoherencyService {

  // We use a custom thread pool to avoid blocking the default one
  private static final ForkJoinPool STORAGE_POOL = new ForkJoinPool(Utils.CPUS * 2);

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String CHECK_COHERENCY_FAILED = "Check logbook coherency failed";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final AdminService adminService;

  // Check coherency
  public Long checkCoherency(Long tenant, String user, String app, int delay, int duration) {
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    OperationDb ope = OperationFactory.checkCoherencyOp(tenant, user, app, delay, duration);
    ope.setStatus(OperationStatus.INIT);
    ope.setMessage("Checking coherency");
    ope = operationService.save(ope);

    // Create and submit task
    processingService.submit(new CoherencyTask(ope, this));
    return ope.getId();
  }

  public void check(OperationDb operation) {
    String user = operation.getUserIdentifier();
    String app = operation.getApplicationId();
    List<Long> opeIds = new ArrayList<>();

    try {
      for (TenantDb tenantDb : tenantService.getTenantsDb()) {
        OperationDb ope = OperationFactory.checkTenantCoherencyOp(tenantDb.getId(), user, app);
        ope = operationService.save(ope);
        processingService.submit(new CoherencyTenantTask(ope, this));
        opeIds.add(ope.getId());
        // TODO The // must be configurable
        if (opeIds.size() > 128) {
          opeIds.forEach(this::waitOperation);
          opeIds.clear();
        }
      }
      opeIds.forEach(this::waitOperation);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Checking Coherency done with success");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      String msg = ExceptionsUtils.format(CHECK_COHERENCY_FAILED, e, operation);
      log.error(msg, ex);
      operationService.unlockAndSave(
          operation,
          OperationStatus.FATAL,
          String.format("Failed to check coherency - Code: %s", e.getCode()));
      // TODO Cancel/delete all remaining operationIds
    }
  }

  public void checkTenant(OperationDb operation) {
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try {
      checkLogbooks(tenantDb, offers);
      checkArchives(tenantDb, offers);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Checking Coherency done with success");

    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      String msg = ExceptionsUtils.format(CHECK_COHERENCY_FAILED, e, operation);
      log.error(msg, ex);
      operationService.unlockAndSave(
          operation,
          OperationStatus.FATAL,
          String.format("Failed to check coherency - Code: %s", e.getCode()));
      // TODO Cancel/delete all remaining operationIds
    }
  }

  private void checkLogbooks(TenantDb tenantDb, List<String> offers) throws IOException {

    // TODO Check the internal checksum of succesive logbook
    if (offers.size() > 1) {
      Long tenant = tenantDb.getId();

      // Get the secure number logbook
      long maxSecureNumber = storageService.getSecureNumber(tenant);

      // Check logbook size
      String oldOffer = null;
      List<Long> logbookIds = null;

      for (String offer : tenantDb.getStorageOffers()) {
        List<Long> ids =
            storageService.findLbkIds(tenantDb, List.of(offer)).stream()
                .filter(n -> n <= maxSecureNumber)
                .toList();
        if (logbookIds != null && logbookIds.size() != ids.size()) {
          throw new InternalException(
              CHECK_COHERENCY_FAILED,
              String.format(
                  "Failed to check logbook size - tenant: %s - offer: %s - size: %s / offer: %s - size: %s",
                  tenant, oldOffer, logbookIds.size(), offer, ids.size()));
        }
        oldOffer = offer;
        logbookIds = ids;
      }

      // Check logbook checksum
      if (logbookIds != null) {
        for (Long id : logbookIds) {
          byte[] checksum = null;
          for (String offer : tenantDb.getStorageOffers()) {
            byte[] cs =
                storageService.getChecksum(
                    tenant, List.of(offer), id, StorageObjectType.lbk, Hash.SHA256);
            if (checksum != null && !Arrays.equals(checksum, cs)) {
              throw new InternalException(
                  CHECK_COHERENCY_FAILED,
                  String.format(
                      "Failed to check logbook checksum - tenant: %s - offer: %s - id: %s",
                      tenant, offer, id));
            }
            checksum = cs;
          }
        }
      }
    }
  }

  // The file checksum will never be modified during the check because update, delete operations are
  // exclusive
  private void checkArchives(TenantDb tenantDb, List<String> offers)
      throws IOException, ExecutionException, InterruptedException {

    Long tenant = tenantDb.getId();
    // We need to stop checking at some time
    long operationId = operationService.findLastArchiveOperationIdToProcess(tenant);
    long capacity = adminService.getActionsSize(tenantDb, offers, operationId);
    log.info("Tenant {} - Capacity {}", tenant, capacity);

    if (capacity > 0) {
      try (StorageObjectSet storageObjectSet = OperationUtils.createStorageObjectSet(capacity)) {
        IteratorFactory factory = new CoherencyIteratorFactory(storageService, operationService);
        OperationProcessor processor = new CoherencyProcessor(storageObjectSet);
        OperationUtils.scanDb(operationId, tenant, factory, processor);
        OperationUtils.scanLbk(tenantDb, offers, factory, processor);

        // Chronicle Map Iterator is not thread safe and must only be accessed from a single thread
        // So we copy chunk of map data into a list before processing
        for (Iterator<List<HashStorageObject>> it = storageObjectSet.listIterator();
            it.hasNext(); ) {
          List<HashStorageObject> hashStorageObjects = it.next();
          STORAGE_POOL.submit(checkChecksum(offers, tenant, hashStorageObjects)).get();
        }
      }
    }
  }

  private Runnable checkChecksum(List<String> offers, Long tenant, List<HashStorageObject> sos) {
    return () -> sos.parallelStream().forEach(so -> checkChecksum(offers, tenant, so));
  }

  @SneakyThrows
  private void checkChecksum(List<String> offers, Long tenant, HashStorageObject so) {
    // Maybe we could run each offer request on a dedicated thread, but...
    // 1. call to this method is already //
    // 2. offers are shuffled to optimize IO concurrency
    // 3. thread creation takes time, so test before implementing //   .

    List<String> shuffleOffers = offers.size() > 1 ? CollUtils.shuffleList(offers) : offers;
    for (String offer : shuffleOffers) {
      byte[] checksum =
          storageService.getChecksum(
              tenant, List.of(offer), so.getId(), so.getType(), so.getHash());

      if (!Arrays.equals(so.getChecksum(), checksum)) {
        throw new InternalException(
            "Check digest in store operation batch failed",
            String.format(
                "Checksum not equals for tenant: %s - offer: %s - id: %s - type: %s - logbook/db checksum: %s - offer checksum: %s",
                tenant,
                offer,
                so.getId(),
                so.getType(),
                HashUtils.encodeHex(so.getChecksum()),
                HashUtils.encodeHex(checksum)));
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
}
