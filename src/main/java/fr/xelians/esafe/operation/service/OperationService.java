/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.service;

import static fr.xelians.esafe.operation.domain.OperationStatus.*;
import static fr.xelians.esafe.operation.domain.OperationType.*;

import fr.xelians.esafe.archive.domain.search.search.Hits;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationQuery;
import fr.xelians.esafe.operation.dto.OperationResult;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.dto.vitam.VitamOperationDto;
import fr.xelians.esafe.operation.dto.vitam.VitamOperationListDto;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.entity.TaskLockDb;
import fr.xelians.esafe.operation.repository.OperationRepository;
import fr.xelians.esafe.operation.repository.SeqLbkRepository;
import fr.xelians.esafe.operation.repository.TaskLockRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import software.amazon.awssdk.http.HttpStatusCode;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationService {

  public static final String TENANT_MUST_BE_NOT_NULL = "tenant must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "id must be not null";
  public static final String OPERATION_NOT_FOUND = "Operation not found";
  public static final String ID_NOT_FOUND = "Operation with id '%s' not found";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String PAGE_REQUEST_MUST_BE_NOT_NULL = "Page request must be not null";
  public static final String LOCK_NOT_FOUND = "Lock for tenant %s not found";

  public static final int MAX_200 = 200;
  public static final int MAX_1000 = 1000;

  private static final PageRequest FIRST_1 = PageRequest.of(0, 1);
  private static final PageRequest FIRST_200 = PageRequest.of(0, MAX_200);
  private static final PageRequest FIRST_1000 = PageRequest.of(0, MAX_1000);

  private static final List<OperationType> COPY_TYPES =
      List.of(INGEST_ARCHIVE, INGEST_HOLDING, INGEST_FILING, ELIMINATE_ARCHIVE);
  private static final List<OperationType> CAPACITY_TYPES =
      List.of(INGEST_ARCHIVE, INGEST_HOLDING, INGEST_FILING);
  private static final List<OperationType> CHECK_TYPES =
      List.of(
          INGEST_ARCHIVE,
          INGEST_HOLDING,
          INGEST_FILING,
          UPDATE_ARCHIVE,
          UPDATE_ARCHIVE_RULES,
          RECLASSIFY_ARCHIVE,
          ELIMINATE_ARCHIVE);
  private static final List<OperationType> ARCHIVE_TYPES =
      List.of(
          INGEST_ARCHIVE,
          INGEST_HOLDING,
          INGEST_FILING,
          UPDATE_ARCHIVE,
          UPDATE_ARCHIVE_RULES,
          RECLASSIFY_ARCHIVE,
          ELIMINATE_ARCHIVE);

  private static final List<OperationStatus> STORE_INDEX_OK_STATUS = List.of(STORE, INDEX, OK);
  private static final List<OperationStatus> RUN_STORE_INDEX_STATUS =
      List.of(RUN, STORE, RETRY_STORE, INDEX, RETRY_INDEX);
  private static final List<OperationStatus> INDEX_OK_STATUS = List.of(INDEX, RETRY_INDEX, OK);
  private static final List<OperationStatus> RUN_STORE_STATUS = List.of(RUN, STORE, RETRY_STORE);
  private static final Hits ONE_HITS = new Hits(0, 1, 1, 1);

  private final OperationRepository operationRepository;
  private final TaskLockRepository lockRepository;
  private final SeqLbkRepository seqLbkRepository;

  public OperationDb save(OperationDb operation) {
    operation.setModified(LocalDateTime.now());
    return operationRepository.save(operation);
  }

  // Dto Search operations - Dto are not stored in the  entity manager cache
  public OperationDto getOperationDto(Long tenant, Long id) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    return operationRepository
        .getOperationDto(tenant, id)
        .orElseThrow(
            () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
  }

  public OperationStatus getOperationStatusDto(Long id) {
    return operationRepository
        .getOperationStatusDto(id)
        .orElseThrow(
            () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
  }

  public OperationStatusDto getOperationStatusDto(Long tenant, Long id) {
    return operationRepository
        .getOperationStatusDto(tenant, id)
        .orElseThrow(
            () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
  }

  public OperationResult<VitamOperationDto> getVitamOperationDto(Long tenant, Long id) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    VitamOperationDto dto =
        operationRepository
            .getVitamOperationDto(tenant, id)
            .orElseThrow(
                () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
    return new OperationResult<>(HttpStatusCode.OK, ONE_HITS, List.of(dto), "{}");
  }

  public SliceResult<OperationDto> searchOperationDtos(
      Long tenant, OperationQuery operationQuery, PageRequest pageRequest) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(operationQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(pageRequest, PAGE_REQUEST_MUST_BE_NOT_NULL);

    return new SliceResult<>(
        operationRepository.searchOperationDtos(tenant, operationQuery, pageRequest));
  }

  public OperationResult<VitamOperationListDto> searchVitamOperationDtos(
      Long tenant, OperationQuery operationQuery) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(operationQuery, QUERY_MUST_BE_NOT_NULL);

    PageRequest pageRequest = SearchUtils.createPageRequest(0, MAX_1000);
    Slice<OperationDto> results =
        operationRepository.searchOperationDtos(tenant, operationQuery, pageRequest);
    return new OperationResult<>(
        HttpStatus.OK.value(),
        new Hits(0, MAX_1000, results.getContent().size(), results.getContent().size()),
        results.getContent().stream().map(VitamOperationListDto::new).toList(),
        JsonService.toString(operationQuery));
  }

  public SliceResult<OperationStatusDto> searchOperationStatusDto(
      Long tenant, OperationQuery operationQuery, PageRequest pageRequest) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(operationQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(pageRequest, PAGE_REQUEST_MUST_BE_NOT_NULL);

    return new SliceResult<>(
        operationRepository.findOperationStatus(tenant, operationQuery, pageRequest));
  }

  // Get operations
  public OperationDb getOperationDb(Long tenant, Long id) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    return operationRepository
        .findOneByTenantAndId(tenant, id)
        .orElseThrow(
            () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
  }

  public String getContractIdentifier(Long tenant, Long id) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    return operationRepository
        .getContractIdentifier(tenant, id)
        .orElseThrow(
            () -> new NotFoundException(OPERATION_NOT_FOUND, String.format(ID_NOT_FOUND, id)));
  }

  public List<OperationDb> findByStatus(OperationStatus status1) {
    return operationRepository.findFirst2000ByStatusOrderByIdAsc(status1);
  }

  public List<OperationDb> findByStatus(OperationStatus status1, OperationStatus status2) {
    return operationRepository.findFirst2000ByStatusOrStatusOrderByIdAsc(status1, status2);
  }

  public List<OperationDb> findByTenantAndTypeAndStatus(
      Long tenant, OperationType type, OperationStatus status) {
    return operationRepository.findByTenantAndTypeAndStatusOrderByIdAsc(tenant, type, status);
  }

  public List<OperationDb> findArchiveOps(Long tenant) {
    return operationRepository.find(tenant, ARCHIVE_TYPES);
  }

  // Find the last (greater) archive operation id that is being indexed or ok
  public long findLastArchiveOperationIdToProcess(Long tenant) {
    return operationRepository.findDesc(tenant, ARCHIVE_TYPES, INDEX_OK_STATUS, FIRST_1).stream()
        .findFirst()
        .orElse(-1L);
  }

  // Find operations greater than id in the database that need securing
  public List<OperationDb> findForSecuring(Long tenant, Long lbkId, LocalDateTime startDate) {
    // Normally, it would be better to secure all operations with status INDEX and OK.
    // However, an operation not indexed means either a search engine problem
    // or an indexation bug. For this last reason, it's (now) preferable to only secure OK
    // operations
    return operationRepository.findToSecure(tenant, lbkId, OK, startDate, FIRST_1000);
  }

  @Transactional
  public void updateSecured(OperationDb operation) {
    operationRepository.updateSecured(operation.getId(), false);
  }

  public List<OperationDb> findToRegister() {
    return operationRepository.findToRegister(OK);
  }

  @Transactional
  public void updateRegistered(OperationDb operation) {
    operationRepository.updateRegister(operation.getId(), false);
  }

  // Find operations
  public List<Long> findFirstIdForExclusive(Long tenant) {
    return operationRepository.findFirst(tenant, ARCHIVE_TYPES, RUN_STORE_INDEX_STATUS, FIRST_1);
  }

  // Find operations greater than idMin and less than idMax for reindex scanner
  public List<OperationDb> findForReindex(Long tenant, Long idMin, Long idMax) {
    return operationRepository.findForReindex(tenant, idMin, idMax, OK, FIRST_200);
  }

  // Find operations greater than idMin and less than idMax for copy scanner
  public List<OperationDb> findForCopy(Long tenant, Long idMin, Long idMax) {
    return operationRepository.find(
        tenant, idMin, idMax, COPY_TYPES, STORE_INDEX_OK_STATUS, FIRST_200);
  }

  // Find operations greater than idMin and less than idMax for capacity scanner
  public List<OperationDb> findForCapacity(Long tenant, Long idMin, Long idMax) {
    return operationRepository.find(
        tenant, idMin, idMax, CAPACITY_TYPES, STORE_INDEX_OK_STATUS, FIRST_200);
  }

  // Find operations greater than idMin and less than idMax for check coherency scanner
  public List<OperationDb> findForCheck(Long tenant, Long idMin, Long idMax) {
    return operationRepository.find(
        tenant, idMin, idMax, CHECK_TYPES, STORE_INDEX_OK_STATUS, FIRST_200);
  }

  // Update Operations
  @Transactional
  public void updateStatusAndMessage(
      OperationStatus status, OperationStatus newStatus, String newMessage) {
    operationRepository.updateStatusAndMessage(status, newStatus, newMessage);
  }

  public List<Long> findOperationIds(OperationStatus status, LocalDateTime date) {
    return operationRepository.findOperationIds(status, date);
  }

  public List<Long> findCompletedOperationIds(OperationStatus status, LocalDateTime date) {
    return operationRepository.findCompletedOperationIds(status, date);
  }

  @Transactional
  public void updateByStatusAndDate(
      OperationStatus status, LocalDateTime date, OperationStatus newStatus, String newDetail) {
    operationRepository.updateStatusAndMessage(status, date, newStatus, newDetail);
  }

  // Delete Operations
  public void deleteOperations(Long operationId) {
    operationRepository.deleteById(operationId);
  }

  // Wait for secured log operations that started before this task and not yet stored on offers
  // This ensures that next storage logs will be written on all offers
  public void waitForSecureOperationsToBeStored(Long tenant) {
    // Find all secure operations that are not yet stored on storage offers
    List<OperationDb> operations =
        operationRepository.findByTenantAndTypeAndStatusOrderByIdAsc(tenant, TRACEABILITY, RUN);
    operations.forEach(ope -> waitOperation(ope.getId(), 1_800_000, INDEX, RETRY_INDEX, OK, FATAL));
  }

  // Wait for archive operations that started before this task and not yet stored on offers
  // This ensures that next archives will be written on all offers
  public void waitForArchiveOperationsToBeStored(Long tenant) {
    // Find all archive operations that are not yet stored on storage offers
    List<OperationDb> operations =
        operationRepository.find(tenant, ARCHIVE_TYPES, RUN_STORE_STATUS);
    operations.forEach(
        ope ->
            waitOperation(
                ope.getId(),
                1_800_000,
                ERROR_INIT,
                ERROR_CHECK,
                ERROR_COMMIT,
                INDEX,
                RETRY_INDEX,
                OK));
  }

  public OperationStatus waitOperation(Long id, long timeout, OperationStatus... operationStatus) {
    long delay = 0;
    while (delay < timeout) {
      log.info("getOperationStatus " + id);
      OperationStatus status = getOperationStatusDto(id);
      log.info("getOperationStatus done " + id);
      for (OperationStatus s : operationStatus) {
        if (status == s) {
          return status;
        }
      }
      // TODO Must be configurable
      Utils.sleep(100);
      delay += 100L;
    }
    throw new InternalException(
        "Wait for operation failed",
        String.format("Failed to get operation status '%s' - time out", id));
  }

  @Transactional
  public boolean isLocked(Long tenant) {
    return lockRepository
        .findForRead(tenant)
        .orElseThrow(
            () ->
                new InternalException(
                    "Failed to get task lock", String.format(LOCK_NOT_FOUND, tenant)))
        .isExclusiveLock();
  }

  // return true if already locked or if running
  @Transactional
  public boolean tryLock(OperationDb operation) {
    Long tenant = operation.getTenant();

    if (operation.isExclusive()) {
      // Fails if a read or write lock already exists (except in the same transaction) and prevents
      // all locks
      TaskLockDb taskLockDb =
          lockRepository
              .findForUpdate(tenant)
              .orElseThrow(
                  () ->
                      new InternalException(
                          "Failed to acquire write lock", String.format(LOCK_NOT_FOUND, tenant)));
      if (taskLockDb.isExclusiveLock()) {
        return true;
      }
      // Find running or retrying operations
      List<Long> ids = findFirstIdForExclusive(tenant);
      if (!ids.isEmpty()) {
        ids.forEach(i -> log.info("Waiting for operations {} to finish", i));
        return true;
      }
      taskLockDb.setExclusiveLock(true);
    } else {
      // Fails if a write lock already exists and prevents write lock
      TaskLockDb taskLockDb =
          lockRepository
              .findForRead(tenant)
              .orElseThrow(
                  () ->
                      new InternalException(
                          "Failed to acquire read lock", String.format(LOCK_NOT_FOUND, tenant)));
      if (taskLockDb.isExclusiveLock()) {
        return true;
      }
    }

    operation.setStatus(OperationStatus.RUN);
    save(operation);
    return false;
  }

  /*
   * This method guarantee the execution order of exclusive operations by the setting of the lbkId
   * property (but does not guarantee the order of the non-exclusive operations). This is necessary
   * in order to write the operation in the right order to the operation logbook.
   */
  @Transactional
  public void unlockAndSave(OperationDb operation, OperationStatus status, String message) {
    if (operation.isExclusive()) {
      Long tenant = operation.getTenant();
      TaskLockDb taskLockDb =
          lockRepository
              .findForUpdate(tenant)
              .orElseThrow(
                  () ->
                      new InternalException(
                          "Failed to release lock", String.format(LOCK_NOT_FOUND, tenant)));

      if (taskLockDb.isExclusiveLock()) {
        taskLockDb.setExclusiveLock(false);
      } else {
        InternalException ex = new InternalException("Unlock operation failed");
        log.error(ExceptionsUtils.format(ex, operation));
      }
    }
    operation.setLbkId(seqLbkRepository.getNextValue());
    operation.setStatus(status);
    operation.setMessage(message);
    operation.setModified(LocalDateTime.now());
    operationRepository.save(operation);
  }
}
