/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import static fr.xelians.esafe.common.utils.ExceptionsUtils.format;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.updaterule.RuleLists;
import fr.xelians.esafe.archive.domain.search.updaterule.RuleTypeName;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRuleParser;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRuleQuery;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRules;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRulesRequest;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRulesResult;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.archive.task.UpdateRulesTask;
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
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.entity.RuleDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.referential.service.RuleService;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class UpdateRulesService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";

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
  private final RuleService ruleService;

  public Long updateRules(
      Long tenant, String accessContract, UpdateRuleQuery ruleQuery, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(ruleQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(ruleQuery);
    OperationDb operation =
        OperationFactory.updateArchiveRulesOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new UpdateRulesTask(operation, this));
    return operation.getId();
  }

  // Select units and write updated units in a temporary units file
  public Path check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();
    if (tenantDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          "Failed to update rules", String.format("Tenant '%s' is not active", tenant));
    }

    Map<String, RuleDb> ruleMap = new HashMap<>();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // Search selected units
      String query = operation.getProperty02();
      UpdateRulesResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();
      RuleLists ruleLists = result.ruleLists();

      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();
      List<ArchiveUnit> modifiedUnits = new ArrayList<>();
      List<ArchiveUnit> removeUnits = new ArrayList<>();
      Map<Long, JsonNode> jsonUnitMap = new HashMap<>();

      // Group updated selected units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      String pit = null;
      Path tmpAusPath = Workspace.createTempFile(operation);
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
            Management mgt = indexedUnit.getManagement();
            if (mgt != null) {
              jsonUnitMap.put(indexedUnitId, jsonUnit);

              boolean isModified = deleteRules(mgt, ruleLists.deleteRules());
              isModified |= addRules(tenant, mgt, ruleLists.addRules());
              isModified |= updateRules(tenant, mgt, ruleLists.updateRules());

              if (isModified) {
                removeEmptyRules(indexedUnit);
                modifiedUnits.add(indexedUnit);
              }
              archiveUnits.add(indexedUnit);
            }
          }
        }

        // Find units with descending ancestor
        Map<Long, ArchiveUnit> modifiedUnitMap = UnitUtils.mapById(modifiedUnits);
        for (ArchiveUnit unit : archiveUnits) {
          for (Long parentId : unit.getParentIds()) {
            if (parentId != -1 && modifiedUnitMap.containsKey(parentId)) {
              removeUnits.add(unit);
            }
          }
        }

        // Remove units with descending ancestor from top units
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
        addLifeCycle(context, archiveUnits, jsonUnitMap, sequenceWriter);

      } finally {
        searchService.closePointInTime(pit);
      }
      return tmpAusPath;
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  // Copy temporary units file to offers
  public void commit(OperationDb operation, TenantDb tenantDb, Path tmpAusPath) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    List<StorageObject> storageObjects = new ArrayList<>();

    // Write Archive Units (.aus) from temporary file
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

  // Write units from temporary file on offers then index units
  public void store(OperationDb operation, TenantDb tenantDb, Path tmpAusPath) {
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = Files.newInputStream(tmpAusPath)) {
      doStore(operation, tenantDb, ausStream, storageDao);
    } catch (IOException ex) {
      NioUtils.deleteDirQuietly(tmpAusPath);
      throw new InternalException(ex);
    }
  }

  // Index operation
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
      operation.setMessage("Failed to update archive unit. Waiting for automatic retry.");
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

  // Index units from operation (in case of batch retry)
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

  private UpdateRulesResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    // Refreshing an index is usually not recommended. We could have indexed all
    // documents with refresh=wait_for|true property. Unfortunately this is very costly
    // in case of mass ingest/update.
    searchService.refresh();

    UpdateRuleQuery updateRuleQuery = ArchiveUnitQueryFactory.createUpdateRuleQuery(query);
    UpdateRuleParser updateParser = UpdateRuleParser.create(tenant, accessContract, ontologyMapper);

    try {
      UpdateRulesRequest updateRulesRequest = updateParser.createRequest(updateRuleQuery);

      SearchRequest searchRequest = updateRulesRequest.searchRequest();
      log.info("Update JSON  - request: {}", JsonUtils.toJson(searchRequest));

      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(searchRequest, ArchiveUnit.class);

      // TODO check if detail overflows
      List<ArchiveUnit> nodes = response.hits().hits().stream().map(Hit::source).toList();
      return new UpdateRulesResult<>(nodes, updateRulesRequest.ruleLists());

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed",
          String.format("Failed to parse query '%s'", updateRuleQuery),
          ex);
    }
  }

  private boolean deleteRules(Management mgt, List<RuleTypeName> actionRules) {
    boolean isModified = false;
    for (RuleTypeName actionRule : actionRules) {
      AbstractRules arules = mgt.getRules(actionRule.ruleType());
      if (arules != null) {
        String ruleName = actionRule.ruleName();
        isModified |= arules.deleteRule(ruleName); // bitwise operator works for boolean
      }
    }
    return isModified;
  }

  private boolean addRules(Long tenant, Management mgt, List<AbstractRules> actionRules) {
    boolean isModified = false;
    Map<String, RuleDb> ruleMap = new HashMap<>();

    for (AbstractRules actionRule : actionRules) {
      actionRule.getRules().forEach(rule -> checkRule(ruleMap, tenant, rule.getRuleName()));
      mgt.setRules(actionRule);
      isModified = true;
    }
    return isModified;
  }

  private boolean updateRules(Long tenant, Management mgt, Map<RuleType, UpdateRules> actionRules) {

    boolean isModified = false;
    Map<String, RuleDb> ruleMap = new HashMap<>();

    for (Map.Entry<RuleType, UpdateRules> actionRule : actionRules.entrySet()) {
      AbstractRules aRules = mgt.getRules(actionRule.getKey());
      UpdateRules uRules = actionRule.getValue();

      if (uRules.preventInheritance() != null) {
        isModified = true;
        aRules.getRuleInheritance().setPreventInheritance(uRules.preventInheritance());
      }

      if (aRules instanceof FinalActionRule far && StringUtils.isNotBlank(uRules.finalAction())) {
        isModified = true;
        far.setFinalAction(uRules.finalAction());
      }

      // TODO Add classification properties
      if (aRules instanceof ClassificationRules cr) {
        // Set the values from uRules
      }

      Map<String, Rule> aRuleMap =
          aRules.getRules().stream()
              .collect(Collectors.toMap(Rule::getRuleName, Function.identity()));

      for (var uRule : uRules.rules()) {
        Rule rule = aRuleMap.get(uRule.oldRule());
        if (rule == null) continue;

        isModified = true;

        if (StringUtils.isNotBlank(uRule.rule())) {
          checkRule(ruleMap, tenant, uRule.rule());
          rule.setRuleName(uRule.rule());
        }
        if (BooleanUtils.isTrue(uRule.deleteStartDate())) {
          rule.setStartDate(null);
        }
        if (uRule.startDate() != null) {
          rule.setStartDate(uRule.startDate());
        }

        if (rule instanceof HoldRule holdRule) {
          if (BooleanUtils.isTrue(uRule.deleteHoldEndDate())) {
            holdRule.setHoldEndDate(null);
          }
          if (uRule.holdEndDate() != null) {
            holdRule.setHoldEndDate(uRule.holdEndDate());
          }
          if (BooleanUtils.isTrue(uRule.deleteHoldReassessingDate())) {
            holdRule.setHoldReassessingDate(null);
          }
          if (uRule.holdReassessingDate() != null) {
            holdRule.setHoldReassessingDate(uRule.holdReassessingDate());
          }
          if (BooleanUtils.isTrue(uRule.deleteHoldOwner())) {
            holdRule.setHoldOwner(null);
          }
          if (StringUtils.isNotBlank(uRule.holdOwner())) {
            holdRule.setHoldOwner(uRule.holdOwner());
          }
          if (BooleanUtils.isTrue(uRule.deleteHoldReason())) {
            holdRule.setHoldReason(null);
          }
          if (StringUtils.isNotBlank(uRule.holdReason())) {
            holdRule.setHoldReason(uRule.holdReason());
          }
          if (BooleanUtils.isTrue(uRule.deletePreventRearrangement())) {
            holdRule.setPreventRearrangement(null);
          }
          if (uRule.preventRearrangement() != null) {
            holdRule.setPreventRearrangement(uRule.deletePreventRearrangement());
          }
        }
      }
    }
    return isModified;
  }

  private void checkRule(Map<String, RuleDb> ruleMap, Long tenant, String ruleName) {
    // Throw a NotFound exception if rule does not exist
    RuleDb ruleDb = ruleMap.computeIfAbsent(ruleName, id -> ruleService.getEntity(tenant, id));

    if (ruleDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          "Failed tu update rules", String.format("Rule '%s' is not active", ruleName));
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

  private void addLifeCycle(
      Context context,
      List<ArchiveUnit> archiveUnits,
      Map<Long, JsonNode> jsonUnitMap,
      SequenceWriter sequenceWriter)
      throws IOException {

    Long operationId = context.operationDb().getId();
    LocalDateTime created = LocalDateTime.now();
    OntologyMapper ontologyMapper = context.ontologyMapper();

    for (ArchiveUnit unit : archiveUnits) {
      JsonNode jsonUnit = jsonUnitMap.get(unit.getId());
      unit.setUpdateDate(created);
      JsonNode patchedJsonUnit = JsonService.toJson(unit);

      JsonNode revertPatchNode = JsonDiff.asJson(patchedJsonUnit, jsonUnit);
      String revertPatch = JsonService.toString(revertPatchNode);
      unit.addToOperationIds(operationId);
      unit.addLifeCycle(
          new LifeCycle(
              unit.getAutoversion(),
              operationId,
              OperationType.UPDATE_ARCHIVE_RULES,
              created,
              revertPatch));

      unit.buildProperties(ontologyMapper);
      sequenceWriter.write(unit);
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
        new ArchiveReporter(ReportType.UPDATE_RULE, ReportStatus.OK, operation, reportPath)) {

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

  // TODO finaliser le update rule. Add  delete et update (bien parser avec les valeurs)

  // Reset rules that have no rule and no prevent inheritance

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
      // id
      // Check storage and index coherency. Could be optional
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
            OperationType.UPDATE_ARCHIVE_RULES,
            created,
            revertPatch));
    patchedUnit.buildProperties(context.ontologyMapper());
  }
}
