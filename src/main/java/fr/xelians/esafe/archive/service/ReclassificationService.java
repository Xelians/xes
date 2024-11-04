/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import static fr.xelians.esafe.common.utils.ExceptionsUtils.format;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.reclassification.ReclassificationParser;
import fr.xelians.esafe.archive.domain.search.reclassification.ReclassificationRequest;
import fr.xelians.esafe.archive.domain.search.reclassification.ReclassificationResult;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.archive.domain.unit.rules.computed.HoldComputedRules;
import fr.xelians.esafe.archive.task.ReclassificationTask;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.common.utils.Context;
import fr.xelians.esafe.common.utils.ListIterator;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.*;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class ReclassificationService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String FAILED_TO_RECLASSIFY = "Failed to reclassify archives";

  private final ProcessingService processingService;
  private final SearchService searchService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final LogbookService logbookService;
  private final SearchEngineService searchEngineService;
  private final IndexService indexService;
  private final DateRuleService dateRuleService;
  private final AccessContractService accessContractService;
  private final OntologyService ontologyService;

  public Long reclassify(
      Long tenant, String accessContract, UpdateQuery updateQuery, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(updateQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(updateQuery);
    OperationDb operation =
        OperationFactory.reclassifyArchiveOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new ReclassificationTask(operation, this));
    return operation.getId();
  }

  // Index archive units from operation and storage. This is not the usual case
  public void indexOperation(OperationDb operation) {
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // Get all operation ids
      List<Long> ids =
          operation.getActions().stream()
              .map(StorageAction::create)
              .map(StorageObjectId::getId)
              .toList();
      indexService.indexArchives(storageDao, tenant, offers, ids);
      searchService.refresh();

      // Index Operation in Search Engine
      logbookService.index(operation);
      // Save Operation to Db
      operationService.unlockAndSave(operation, OperationStatus.OK, "Index done successfully");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Index archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Failed to index archive units. Waiting for automatic retry.");
      operationService.save(operation);
    }
  }

  public void storeOperation(OperationDb operation) {
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    Long operationId = operation.getId();
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = storageDao.getAusStream(tenant, offers, operationId)) {
      doStore(operation, tenantDb, ausStream, storageDao);
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(ExceptionsUtils.format("Reclassify archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_STORE);
      operation.setMessage("Failed to reclassify archive unit. Waiting for automatic retry.");
      operationService.save(operation);
      return;
    }

    try {
      logbookService.index(operation);
      operationService.unlockAndSave(operation, OperationStatus.OK, "Index done successfully");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(ExceptionsUtils.format("Reclassify archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Failed to index archive units. Waiting for automatic retry.");
      operationService.save(operation);
    }

    // TODO : purge aus files
  }

  public void store(OperationDb operation, TenantDb tenantDb, Path tmpAusPath) {
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = Files.newInputStream(tmpAusPath)) {
      doStore(operation, tenantDb, ausStream, storageDao);
    } catch (IOException ex) {
      NioUtils.deleteDirQuietly(tmpAusPath);
      throw new InternalException(ex);
    }
  }

  private void doStore(
      OperationDb operation, TenantDb tenantDb, InputStream ausStream, StorageDao storageDao)
      throws IOException {
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    boolean isFirst = true;
    boolean needRefresh = false;
    long byteCount = 0;
    List<StorageObject> storageObjects = new ArrayList<>();

    // Reset Actions
    operation.resetActions();

    // Get updated Archive Units from stream
    Iterator<ArchiveUnit> iterator = JsonService.toArchiveUnitIterator(ausStream);
    Iterator<List<ArchiveUnit>> listIterator = ListIterator.iterator(iterator, 10000);
    while (listIterator.hasNext()) {
      List<ArchiveUnit> indexedUnits = listIterator.next();
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(indexedUnits);

      // Write archive units by operation id
      for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
        Long opId = entry.getKey();
        List<ArchiveUnit> groupedUnits = entry.getValue();

        // Read from storage offers archive units with unit.operationId and group them by archive
        // unit id
        Map<Long, ArchiveUnit> storedUnits =
            UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

        // Replace the stored archive with the reclassified unit
        groupedUnits.forEach(unit -> storedUnits.put(unit.getId(), unit));

        byte[] bytes = JsonService.collToBytes(storedUnits.values(), JsonConfig.DEFAULT);
        storageObjects.add(new ByteStorageObject(bytes, opId, StorageObjectType.uni));
        byteCount += bytes.length;

        if (byteCount > 256_000_000) {
          // Commit the created/modified units to offers and create actions
          storageDao
              .putStorageObjects(tenant, offers, storageObjects)
              .forEach(e -> operation.addAction(StorageAction.create(ActionType.UPDATE, e)));
          storageObjects = new ArrayList<>();
          byteCount = 0;
        }
      }

      // Commit the created/modified units to offers and create actions
      if (!storageObjects.isEmpty()) {
        storageDao
            .putStorageObjects(tenant, offers, storageObjects)
            .forEach(e -> operation.addAction(StorageAction.create(ActionType.UPDATE, e)));
      }

      // Index Archive units in Search Engine (properties are already built in each archive)
      if (isFirst && !listIterator.hasNext()) {
        searchService.bulkIndexRefresh(indexedUnits);
      } else {
        searchService.bulkIndex(indexedUnits);
        needRefresh = true;
      }
      isFirst = false;
    }

    if (needRefresh) {
      searchService.refresh();
    }
  }

  public void index(OperationDb operation) {
    try {
      operation.setStatus(OperationStatus.OK);
      operation.setOutcome(operation.getStatus().toString());
      operation.setTypeInfo(operation.getType().getInfo());
      operation.setMessage("Operation completed with success");
      logbookService.index(operation);
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  public Path check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();
    if (tenantDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          FAILED_TO_RECLASSIFY, String.format("Tenant '%s' is not active", tenant));
    }

    Map<String, RuleDb> ruleMap = new HashMap<>();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    // Search Archive Units to update
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // Get Query from property
      String query = operation.getProperty02();
      ReclassificationResult<ArchiveUnit> result =
          search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();
      Long dstId = result.unitUp();

      Path tmpAusPath = Workspace.createTempFile(operation);
      Long operationId = operation.getId();
      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();

      // Get the destination archive unit for all selected archives
      ArchiveUnit dstIndexedUnit = searchService.getLinkedArchiveUnit(tenant, dstId);
      List<Long> dstParentIds = dstIndexedUnit.getParentIds();
      Long dstOpId = dstIndexedUnit.getOperationId();

      // Check storage and index coherency. Could be optional
      ArchiveUnit dstStoredArchiveUnit =
          storageDao.getArchiveUnits(tenant, offers, dstOpId).stream()
              .filter(au -> Objects.equals(au.getId(), dstId))
              .findAny()
              .orElseThrow(
                  () ->
                      new BadRequestException(
                          FAILED_TO_RECLASSIFY,
                          String.format(
                              "Archive unit id '%s' was not found in %s.uni from storage offer",
                              dstId, dstOpId)));

      UnitUtils.checkVersion(dstIndexedUnit, dstStoredArchiveUnit);
      if (!Objects.equals(dstParentIds, dstStoredArchiveUnit.getParentIds())) {
        throw new InternalException(
            FAILED_TO_RECLASSIFY,
            String.format(
                "Destination archive unit '%s' parent ids must be equal '%s' - '%s'",
                dstIndexedUnit, dstParentIds, dstStoredArchiveUnit.getParentIds()));
      }

      // Group updated Archive Units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      // Reclassify top archive units by operation id
      for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
        Long opId = entry.getKey();
        List<ArchiveUnit> indexedUnits = entry.getValue();

        // Read from storage offers archive units with unit.operationId and group them by archive
        // unit id
        Map<Long, ArchiveUnit> storedArchiveUnitMap =
            UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

        // Loop on indexed archive units
        for (ArchiveUnit indexedUnit : indexedUnits) {
          Long archiveUnitId = indexedUnit.getId();

          // Don't reclassify archives with the adequate parent
          if (dstId.equals(indexedUnit.getParentId())) {
            continue;
          }

          // Avoid cyclic dependency
          if (dstParentIds.contains(archiveUnitId)) {
            throw new BadRequestException(
                FAILED_TO_RECLASSIFY,
                String.format(
                    "Parent Archive '%s' cannot be child of archive unit '%s'",
                    dstId, archiveUnitId));
          }

          // Check storage and index coherency. Could be optional
          ArchiveUnit storedUnit = storedArchiveUnitMap.get(archiveUnitId);
          UnitUtils.checkVersion(indexedUnit, storedUnit);

          // Check is reclassification is prevented by hold rule
          checkHoldRule(indexedUnit);

          JsonNode jsonUnit = JsonService.toJson(indexedUnit);

          // Update archive unit
          LocalDateTime created = operation.getCreated();

          dateRuleService.setRulesEndDates(tenant, ruleMap, indexedUnit, dstIndexedUnit);
          indexedUnit.setParentId(dstId);
          indexedUnit.setParentIds(CollUtils.concat(dstId, dstParentIds));
          indexedUnit.setUpdateDate(created);

          // TODO reclassification service producteur

          JsonNode patchedJsonUnit = JsonService.toJson(indexedUnit);
          JsonNode revertPatchNode = JsonDiff.asJson(patchedJsonUnit, jsonUnit);
          String revertPatch = JsonService.toString(revertPatchNode);

          indexedUnit.addToOperationIds(operationId);
          indexedUnit.addLifeCycle(
              new LifeCycle(
                  indexedUnit.getAutoversion(),
                  operationId,
                  OperationType.RECLASSIFY_ARCHIVE,
                  created,
                  revertPatch));
          indexedUnit.buildProperties(ontologyMapper);

          archiveUnits.add(indexedUnit);
        }
      }

      Context context =
          new Context(operation, tenantDb, accessContract, ontologyMapper, ruleMap, tmpAusPath);
      doReclassification(context, archiveUnits, storageDao);
      return tmpAusPath;

    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private ReclassificationResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    UpdateQuery reclassificationQuery = ArchiveUnitQueryFactory.createUpdateQuery(query);
    ReclassificationParser queryParser =
        ReclassificationParser.create(tenant, accessContract, ontologyMapper);

    try {
      ReclassificationRequest reclassificationRequest =
          queryParser.createRequest(reclassificationQuery);

      log.info(
          "Update JSON  - request: {} - value: {} ",
          JsonUtils.toJson(reclassificationRequest.searchRequest()),
          reclassificationRequest.unitUp());

      // Refreshing an index is usually not recommended. We could have indexed all
      // documents with refresh=wait_for|true property. Unfortunately this is very costly
      // in case of mass ingest/update.
      searchService.refresh();

      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(reclassificationRequest.searchRequest(), ArchiveUnit.class);

      // TODO check if detail overflows
      List<ArchiveUnit> nodes = response.hits().hits().stream().map(Hit::source).toList();
      Long unitUp = reclassificationRequest.unitUp();
      return new ReclassificationResult<>(nodes, unitUp);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed",
          String.format("Failed to parse query '%s'", reclassificationQuery),
          ex);
    }
  }

  private void doReclassification(
      Context context, List<ArchiveUnit> parentUnits, StorageDao storageDao) throws IOException {
    // Reclassify children archive units by operation id
    Map<Long, ArchiveUnit> topUnitMap = UnitUtils.mapById(parentUnits);
    String pit = null;

    try (SequenceWriter sequenceWriter =
        JsonService.createSequenceWriter(context.path(), JsonConfig.DEFAULT)) {
      sequenceWriter.writeAll(parentUnits);
      pit = searchService.openPointInTime();
      reclassifyChildren(context, parentUnits, topUnitMap, storageDao, sequenceWriter, pit);
    } finally {
      searchService.closePointInTime(pit);
    }
  }

  private void reclassifyChildren(
      Context context,
      List<ArchiveUnit> parentUnits,
      Map<Long, ArchiveUnit> topUnitMap,
      StorageDao storageDao,
      SequenceWriter sequenceWriter,
      String pitId)
      throws IOException {

    Long tenant = context.tenantDb().getId();
    AccessContractDb accessContract = context.accessContractDb();

    // We limit the search to 1000 terms. Elastic search limit is 65,536
    for (List<ArchiveUnit> partList : ListUtils.partition(parentUnits, 1000)) {

      Map<Long, ArchiveUnit> parentUnitMap = UnitUtils.mapById(partList);
      List<Long> parentIds = partList.stream().map(ArchiveUnit::getId).toList();

      // Search first children of each top archive unit filtering other top archive units (as child)
      Stream<ArchiveUnit> stream =
          searchService
              .searchFirstChildrenStream(tenant, accessContract, parentIds, pitId)
              .filter(unit -> !topUnitMap.containsKey(unit.getId()));

      // Process stream by chunk of 10000 units
      CollUtils.chunk(stream, 10000)
          .forEach(
              childUnits ->
                  reclassifyFirstChildren(
                      context,
                      childUnits,
                      parentUnitMap,
                      topUnitMap,
                      storageDao,
                      sequenceWriter,
                      pitId));
    }
  }

  @SneakyThrows
  private void reclassifyFirstChildren(
      Context context,
      List<ArchiveUnit> childUnits,
      Map<Long, ArchiveUnit> parentUnitMap,
      Map<Long, ArchiveUnit> topUnitMap,
      StorageDao storageDao,
      SequenceWriter sequenceWriter,
      String pitId) {

    Long tenant = context.tenantDb().getId();
    List<String> offers = context.tenantDb().getStorageOffers();

    // Group Archive Units by operation id
    Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(childUnits);

    // Reclassify archive units by operation id
    for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
      Long opId = entry.getKey();
      List<ArchiveUnit> groupedChildUnits = entry.getValue();

      // Read from storage offers archive units with unit.operationId and group them by archive unit
      // id
      // Check storage and index coherency. Could be optional
      Map<Long, ArchiveUnit> storedUnitMap =
          UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

      // Loop on children archive units
      for (ArchiveUnit childUnit : groupedChildUnits) {
        // Check storage and index coherency. Could be optional
        ArchiveUnit storedUnit = storedUnitMap.get(childUnit.getId());
        UnitUtils.checkVersion(childUnit, storedUnit);

        ArchiveUnit parentUnit = parentUnitMap.get(childUnit.getParentId());
        checkHoldRule(childUnit);
        reclassifyChild(context, childUnit, parentUnit);
        sequenceWriter.write(childUnit);
      }
    }
    reclassifyChildren(context, childUnits, topUnitMap, storageDao, sequenceWriter, pitId);
  }

  private void reclassifyChild(Context context, ArchiveUnit childUnit, ArchiveUnit parentUnit) {
    Long tenant = context.tenantDb().getId();
    Long operationId = context.operationDb().getId();
    LocalDateTime created = LocalDateTime.now();
    Map<String, RuleDb> ruleMap = context.ruleMap();

    JsonNode jsonUnit = JsonService.toJson(childUnit);

    dateRuleService.setRulesEndDates(tenant, ruleMap, childUnit, parentUnit);
    childUnit.setParentIds(CollUtils.concat(childUnit.getParentId(), parentUnit.getParentIds()));
    // TODO reclassification service producteur
    childUnit.setUpdateDate(created);

    JsonNode patchedJsonUnit = JsonService.toJson(childUnit);
    JsonNode revertPatchNode = JsonDiff.asJson(patchedJsonUnit, jsonUnit);
    String revertPatch = JsonService.toString(revertPatchNode);

    childUnit.addToOperationIds(operationId);
    childUnit.addLifeCycle(
        new LifeCycle(
            childUnit.getAutoversion(),
            operationId,
            OperationType.RECLASSIFY_ARCHIVE,
            created,
            revertPatch));
    childUnit.buildProperties(context.ontologyMapper());
  }

  // Note.
  // PreventRearrangement is specified for each hold rule.
  // If one of the rule prevents rearrangement, then the computed
  // rules also prevents rearrangement.
  // It means a reclassification operation may be prevented by a not
  // expired rule that does not prevent rearrangement and by another
  // expired rule that prevents rearrangement.
  private static void checkHoldRule(ArchiveUnit childUnit) {
    HoldComputedRules rule = childUnit.getComputedInheritedRules().getHoldComputedRules();
    if (rule != null && BooleanUtils.isTrue(rule.getPreventRearrangement())) {
      LocalDate maxEndDate = rule.getMaxEndDate();
      if (maxEndDate == null || !maxEndDate.isBefore(LocalDate.now())) {
        throw new BadRequestException(
            String.format(
                "Cannot eliminate archive unit %s with hold max end date %s",
                childUnit.getId(), rule.getMaxEndDate()));
      }
    }
  }

  public void commit(OperationDb operation, TenantDb tenantDb, Path tmpAusPath) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    List<StorageObject> storageObjects = new ArrayList<>();

    // Write Archive Units (.aus) to temporary file
    storageObjects.add(new PathStorageObject(tmpAusPath, operationId, StorageObjectType.aus, true));

    // Write Operation (This allows to restore operations from offers to database)
    byte[] ops = JsonService.toBytes(operation, JsonConfig.DEFAULT);
    storageObjects.add(new ByteStorageObject(ops, operationId, StorageObjectType.ope, true));

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      // Write .aus and .ope to offers
      storageDao.putStorageObjects(tenant, offers, storageObjects);
    } catch (IOException ex) {
      // Rollback as much as possible
      storageService.deleteObjectsQuietly(offers, tenant, storageObjects);
      throw new InternalException(ex);
    }
  }
}
