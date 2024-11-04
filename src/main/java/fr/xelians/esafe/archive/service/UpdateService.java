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
import com.flipkart.zjsonpatch.JsonPatch;
import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.update.UpdateParser;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.search.update.UpdateRequest;
import fr.xelians.esafe.archive.domain.search.update.UpdateResult;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.archive.domain.unit.rules.management.AbstractRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.Management;
import fr.xelians.esafe.archive.task.UpdateTask;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class UpdateService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String FAILED_TO_UPDATE = "Failed to update archives";

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

  // Create and submit task
  public Long update(
      Long tenant, String accessContract, UpdateQuery updateQuery, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(updateQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(updateQuery);
    OperationDb operation =
        OperationFactory.updateArchiveOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new UpdateTask(operation, this));
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
      log.error(ExceptionsUtils.format("Update archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_STORE);
      operation.setMessage("Failed to update archive units. Waiting for automatic retry.");
      operationService.save(operation);
      return;
    }

    try {
      logbookService.index(operation);
      operationService.unlockAndSave(operation, OperationStatus.OK, "Index done successfully");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(ExceptionsUtils.format("Update archive units failed", e, operation), ex);
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

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    boolean isFirst = true;
    boolean needRefresh = false;
    long byteCount = 0;
    List<StorageObject> storageObjects = new ArrayList<>();

    // Reset Actions
    operation.resetActions();

    Path reportPath = Workspace.createTempFile(operation);
    try (ArchiveReporter reporter =
        new ArchiveReporter(ReportType.UPDATE, ReportStatus.OK, operation, reportPath)) {

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

          // Replace the stored archive with the updated unit
          for (ArchiveUnit au : groupedUnits) {
            storedUnits.put(au.getId(), au);
            reporter.writeUnit(au);
          }

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
        // TODO Optilmization refresh only if Management has changed
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

    // Write delete report to offer
    List<StorageObject> psois =
        List.of(new PathStorageObject(reportPath, operationId, StorageObjectType.rep));
    storageDao
        .putStorageObjects(tenant, offers, psois)
        .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));
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
          FAILED_TO_UPDATE, String.format("Tenant '%s' is not active", tenant));
    }

    Map<String, RuleDb> ruleMap = new HashMap<>();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      String query = operation.getProperty02();
      UpdateResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();
      JsonNode jsonPatch = result.jsonPatch();

      Path tmpAusPath = Workspace.createTempFile(operation);
      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();
      List<ArchiveUnit> modifiedUnits = new ArrayList<>();
      List<ArchiveUnit> removeUnits = new ArrayList<>();
      Map<Long, JsonNode> jsonUnitMap = new HashMap<>();
      Map<Long, JsonNode> patchedJsonUnitMap = new HashMap<>();

      // Group updated Archive Units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      String pit = null;

      try (SequenceWriter sequenceWriter =
          JsonService.createSequenceWriter(tmpAusPath, JsonConfig.DEFAULT)) {

        // Update top archive units by operation id
        for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
          Long opId = entry.getKey();
          List<ArchiveUnit> indexedArchiveUnits = entry.getValue();

          // Read from storage offers archive units with unit.operationId and group them by archive
          // unit id
          Map<Long, ArchiveUnit> storedUnitMap =
              UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

          // Loop on indexed archive units
          for (ArchiveUnit indexedUnit : indexedArchiveUnits) {
            Long indexedUnitId = indexedUnit.getId();

            // Check storage and index coherency. Could be optional
            ArchiveUnit storedArchiveUnit = storedUnitMap.get(indexedUnitId);
            UnitUtils.checkVersion(indexedUnit, storedArchiveUnit);

            JsonNode jsonUnit = JsonService.toJson(indexedUnit);
            try {
              JsonNode patchedJsonUnit = JsonPatch.apply(jsonPatch, jsonUnit);
              if (!patchedJsonUnit.equals(jsonUnit)) {
                jsonUnitMap.put(indexedUnitId, jsonUnit);
                ArchiveUnit patchedUnit = JsonService.toArchiveUnit(patchedJsonUnit);
                if (!Objects.equals(indexedUnit.getManagement(), patchedUnit.getManagement())) {
                  removeEmptyRules(patchedUnit);
                  modifiedUnits.add(patchedUnit);
                }
                validateUnit(patchedUnit, jsonPatch);
                archiveUnits.add(patchedUnit);
                patchedJsonUnitMap.put(indexedUnitId, patchedJsonUnit);
              }
            } catch (JsonProcessingException | RuntimeException jpe) {
              // Unfortunately the JsonPatchApplicationException thrown by JsonPatch.apply() does
              // not
              // catch all patch errors. So we default to RuntimeException
              throw new BadRequestException(
                  FAILED_TO_UPDATE,
                  String.format("Failed to patch '%s' with '%s'", jsonUnit, jsonPatch),
                  jpe);
            }
          }
        }

        // Find child units with top unit ancestor
        Map<Long, ArchiveUnit> modifiedUnitMap = UnitUtils.mapById(modifiedUnits);
        for (ArchiveUnit unit : archiveUnits) {
          for (Long parentId : unit.getParentIds()) {
            if (parentId != -1 && modifiedUnitMap.containsKey(parentId)) {
              removeUnits.add(unit);
            }
          }
        }

        // Remove child units from top units
        Map<Long, ArchiveUnit> removedUnitMap = UnitUtils.mapById(removeUnits);
        modifiedUnits.removeIf(o -> removedUnitMap.containsKey(o.getId()));
        archiveUnits.removeIf(o -> removedUnitMap.containsKey(o.getId()));

        Context context =
            new Context(operation, tenantDb, accessContract, ontologyMapper, ruleMap, tmpAusPath);

        // Set endDate for top descending units then traverse children tree to set children's
        // endDate
        if (!modifiedUnits.isEmpty()) {
          setRulesEndDate(context, modifiedUnits);
          pit = searchService.openPointInTime();
          updateChildren(
              context, modifiedUnits, removedUnitMap, jsonUnitMap, storageDao, sequenceWriter, pit);
        }

        // Add life cycle to all top units
        addLifeCycle(context, archiveUnits, jsonUnitMap, patchedJsonUnitMap, sequenceWriter);

      } finally {
        searchService.closePointInTime(pit);
      }

      return tmpAusPath;

    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private UpdateResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    UpdateQuery updateQuery = ArchiveUnitQueryFactory.createUpdateQuery(query);
    UpdateParser updateParser = UpdateParser.create(tenant, accessContract, ontologyMapper);

    try {
      UpdateRequest updateRequest = updateParser.createRequest(updateQuery);
      log.info(
          "Update JSON  - request: {} - jsonPatch: {} ",
          JsonUtils.toJson(updateRequest.searchRequest()),
          updateRequest.jsonPatch());

      // Refreshing an index is usually not recommended. We could have indexed all
      // documents with refresh=wait_for|true property. Unfortunately this is very costly
      // in case of mass ingest/update.
      searchService.refresh();

      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(updateRequest.searchRequest(), ArchiveUnit.class);

      // TODO check if detail overflows

      Integer from = updateRequest.searchRequest().from();
      Integer size = updateRequest.searchRequest().size();
      List<ArchiveUnit> nodes = response.hits().hits().stream().map(Hit::source).toList();
      JsonNode jsonPatch = updateRequest.jsonPatch();

      return new UpdateResult<>(query, from, size, nodes, jsonPatch);
    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", updateQuery), ex);
    }
  }

  private static void validateUnit(ArchiveUnit unit, JsonNode jsonPatch) {
    if (unit.getTitle() == null || unit.getTitle().isEmpty()) {
      throw new BadRequestException(
          String.format(
              "Failed to patch '%s' with '%s' - 'Title' cannot be empty", unit, jsonPatch));
    }
  }

  // Reset rules that have no rule and no prevent inheritance
  private static void removeEmptyRules(ArchiveUnit unit) {

    Management mgt = unit.getManagement();
    if (mgt != null) {
      if (checkEmptyRules(mgt.getAppraisalRules())) {
        mgt.setAppraisalRules(null);
      }
      if (checkEmptyRules(mgt.getStorageRules())) {
        mgt.setStorageRules(null);
      }
      if (checkEmptyRules(mgt.getDisseminationRules())) {
        mgt.setDisseminationRules(null);
      }
      if (checkEmptyRules(mgt.getClassificationRules())) {
        mgt.setClassificationRules(null);
      }
      if (checkEmptyRules(mgt.getAccessRules())) {
        mgt.setAccessRules(null);
      }
      if (checkEmptyRules(mgt.getReuseRules())) {
        mgt.setReuseRules(null);
      }
      if (checkEmptyRules(mgt.getHoldRules())) {
        mgt.setHoldRules(null);
      }
    }
  }

  private static boolean checkEmptyRules(AbstractRules arules) {
    return arules != null && arules.isEmpty();
  }

  private void setRulesEndDate(Context context, List<ArchiveUnit> descendingUnits)
      throws IOException {
    Long tenant = context.tenantDb().getId();
    var ruleMap = context.ruleMap();

    List<Long> parentIds =
        descendingUnits.stream()
            .filter(unit -> !unit.isParentRoot())
            .map(ArchiveUnit::getParentId)
            .toList();
    if (parentIds.isEmpty()) {
      for (ArchiveUnit descendingUnit : descendingUnits) {
        dateRuleService.setRulesEndDates(tenant, ruleMap, descendingUnit, null);
      }
    } else {
      Map<Long, ArchiveUnit> parentUnitMap =
          UnitUtils.mapById(searchService.getArchiveUnits(tenant, parentIds));
      for (ArchiveUnit descendingUnit : descendingUnits) {
        dateRuleService.setRulesEndDates(
            tenant, ruleMap, descendingUnit, parentUnitMap.get(descendingUnit.getParentId()));
      }
    }
  }

  private void addLifeCycle(
      Context context,
      List<ArchiveUnit> archiveUnits,
      Map<Long, JsonNode> jsonUnitMap,
      Map<Long, JsonNode> patchedJsonUnitMap,
      SequenceWriter sequenceWriter)
      throws IOException {

    Long operationId = context.operationDb().getId();
    LocalDateTime created = LocalDateTime.now();
    OntologyMapper ontologyMapper = context.ontologyMapper();

    for (ArchiveUnit unit : archiveUnits) {
      JsonNode jsonUnit = jsonUnitMap.get(unit.getId());
      unit.setUpdateDate(created);
      JsonNode patchedJsonUnit = patchedJsonUnitMap.get(unit.getId());

      JsonNode revertPatchNode = JsonDiff.asJson(patchedJsonUnit, jsonUnit);
      String revertPatch = JsonService.toString(revertPatchNode);
      unit.addToOperationIds(operationId);
      unit.addLifeCycle(
          new LifeCycle(
              unit.getAutoversion(),
              operationId,
              OperationType.UPDATE_ARCHIVE,
              created,
              revertPatch));

      unit.buildProperties(ontologyMapper);
      sequenceWriter.write(unit);
    }
  }

  private void updateChildren(
      Context context,
      List<ArchiveUnit> parentUnits,
      Map<Long, ArchiveUnit> removedUnitMap,
      Map<Long, JsonNode> jsonUnitMap,
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
          searchService.searchFirstChildrenStream(tenant, accessContract, parentIds, pitId);

      // Process stream by chunk of 10000 units
      CollUtils.chunk(stream, 10000)
          .forEach(
              childUnits ->
                  updateFirstChildren(
                      context,
                      childUnits,
                      parentUnitMap,
                      removedUnitMap,
                      jsonUnitMap,
                      storageDao,
                      sequenceWriter,
                      pitId));
    }
  }

  @SneakyThrows
  private void updateFirstChildren(
      Context context,
      List<ArchiveUnit> childUnits,
      Map<Long, ArchiveUnit> parentUnitMap,
      Map<Long, ArchiveUnit> removedUnitMap,
      Map<Long, JsonNode> jsonUnitMap,
      StorageDao storageDao,
      SequenceWriter sequenceWriter,
      String pitId) {

    Long tenant = context.tenantDb().getId();
    List<String> offers = context.tenantDb().getStorageOffers();
    Map<String, RuleDb> ruleMap = context.ruleMap();

    // Group Archive Units by operation id
    Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(childUnits);

    // Update archive units by operation id
    for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
      Long opId = entry.getKey();
      List<ArchiveUnit> groupedChildUnits = entry.getValue();

      // Read from storage offers archive units with unit.operationId and group them by archive unit
      // id. Check storage and index coherency. Could be optional
      Map<Long, ArchiveUnit> storedUnitMap =
          UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

      // Loop on children archive units
      for (ArchiveUnit childUnit : groupedChildUnits) {

        // Check storage and index coherency. Could be optional
        ArchiveUnit storedUnit = storedUnitMap.get(childUnit.getId());
        UnitUtils.checkVersion(childUnit, storedUnit);

        JsonNode jsonUnit;
        ArchiveUnit removedUnit = removedUnitMap.get(childUnit.getId());
        if (removedUnit != null) {
          childUnit = removedUnit;
          jsonUnit = jsonUnitMap.get(childUnit.getId());
        } else {
          jsonUnit = JsonService.toJson(childUnit);
        }
        ArchiveUnit parentUnit = parentUnitMap.get(childUnit.getParentId());
        // TODO Check if endDate was modified. If no, don't add LFC and remove unit from childUnits
        dateRuleService.setRulesEndDates(tenant, ruleMap, childUnit, parentUnit);
        addLifeCycle(context, jsonUnit, childUnit);
        sequenceWriter.write(childUnit);
      }
    }
    updateChildren(
        context, childUnits, removedUnitMap, jsonUnitMap, storageDao, sequenceWriter, pitId);
  }

  private void addLifeCycle(Context context, JsonNode jsonUnit, ArchiveUnit patchedUnit) {
    Long operationId = context.operationDb().getId();
    LocalDateTime created = LocalDateTime.now();
    patchedUnit.setUpdateDate(created);

    JsonNode patchedJsonUnit = JsonService.toJson(patchedUnit);
    JsonNode revertPatchNode = JsonDiff.asJson(patchedJsonUnit, jsonUnit);
    String revertPatch = JsonService.toString(revertPatchNode);
    patchedUnit.addToOperationIds(operationId);
    patchedUnit.addLifeCycle(
        new LifeCycle(
            patchedUnit.getAutoversion(),
            operationId,
            OperationType.UPDATE_ARCHIVE,
            created,
            revertPatch));
    patchedUnit.buildProperties(context.ontologyMapper());
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
