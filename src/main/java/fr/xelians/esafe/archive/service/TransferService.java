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
import com.flipkart.zjsonpatch.JsonPatch;
import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.admin.service.AdminService;
import fr.xelians.esafe.archive.domain.export.ExportConfig;
import fr.xelians.esafe.archive.domain.export.Exporter;
import fr.xelians.esafe.archive.domain.export.sedav2.Sedav2Exporter;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import fr.xelians.esafe.archive.domain.search.export.DipExportType;
import fr.xelians.esafe.archive.domain.search.transfer.TransferParser;
import fr.xelians.esafe.archive.domain.search.transfer.TransferQuery;
import fr.xelians.esafe.archive.domain.search.transfer.TransferResult;
import fr.xelians.esafe.archive.domain.search.update.*;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.task.TransferTask;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.ForbiddenException;
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class TransferService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "Id must be not null";
  public static final String FAILED_TO_TRANSFER = "Failed to transfert archives";

  private static final JsonNode JSON_PATCH = createJsonPatch();

  private final AdminService adminService;
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

  @Value("${app.dipexport.maxSize:O}")
  private long maxSize;

  public static JsonNode createJsonPatch() {
    return new JsonPatchBuilder().replace("/_transferred", "true").build();
  }

  public InputStream getDipStream(Long tenant, Long id, String acIdentifier) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);
    Assert.notNull(acIdentifier, ACCESS_CONTRACT_MUST_BE_NOT_NULL);

    String contract = operationService.getContractIdentifier(tenant, id);
    if (acIdentifier.equals(contract)) {
      TenantDb tenantDb = tenantService.getTenantDb(tenant);
      List<String> offers = tenantDb.getStorageOffers();
      try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
        return storageDao.getDipStream(tenant, offers, id);
      }
    }

    throw new ForbiddenException(
        "Failed to download SIP",
        String.format("Access contracts don't match: '%s - '%s'", acIdentifier, contract));
  }

  // Create and submit task
  public Long transfer(
      Long tenant, String accessContract, TransferQuery transferQuery, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(transferQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    // Check if Access Contract exists and is active
    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          FAILED_TO_TRANSFER, String.format("Access Contract '%s' is inactive", accessContractDb));
    }

    String query = JsonService.toString(transferQuery);
    OperationDb operation =
        OperationFactory.transferArchiveOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new TransferTask(operation, this));
    return operation.getId();
  }

  public record TransferPaths(Path ausPath, Path dipPath) {}

  public TransferPaths check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();
    if (tenantDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          FAILED_TO_TRANSFER, String.format("Tenant '%s' is not active", tenant));
    }

    Map<String, RuleDb> ruleMap = new HashMap<>();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getContractIdentifier());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      String query = operation.getProperty02();
      TransferResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();

      Path tmpAusPath = Workspace.createTempFile(operation);
      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();
      Map<Long, JsonNode> jsonUnitMap = new HashMap<>();
      Map<Long, JsonNode> patchedJsonUnitMap = new HashMap<>();

      // Group transferred Archive Units by operation id
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

            if (BooleanUtils.isNotTrue(indexedUnit.getTransferred())) {
              JsonNode jsonUnit = JsonService.toJson(indexedUnit);
              try {
                JsonNode patchedJsonUnit = JsonPatch.apply(JSON_PATCH, jsonUnit);
                jsonUnitMap.put(indexedUnitId, jsonUnit);
                ArchiveUnit patchedUnit = JsonService.toArchiveUnit(patchedJsonUnit);
                archiveUnits.add(patchedUnit);
                patchedJsonUnitMap.put(indexedUnitId, patchedJsonUnit);
              } catch (JsonProcessingException | RuntimeException jpe) {
                // Unfortunately the JsonPatchApplicationException thrown by JsonPatch.apply() does
                // not catch all patch errors. So we default to RuntimeException
                throw new InternalException(
                    FAILED_TO_TRANSFER,
                    String.format("Failed to patch '%s' with '%s'", jsonUnit, JSON_PATCH),
                    jpe);
              }
            }
          }
        }

        Context context =
            new Context(operation, tenantDb, accessContract, ontologyMapper, ruleMap, tmpAusPath);

        // Add life cycle to all top units
        addLifeCycle(context, archiveUnits, jsonUnitMap, patchedJsonUnitMap, sequenceWriter);

      } finally {
        searchService.closePointInTime(pit);
      }

      // Check Dip Size
      checkMaxSize(archiveUnits, result.dataObjectVersionToExport());

      // Export the binary to the tmp path
      ExportConfig exportConfig =
          new ExportConfig(
              DipExportType.FULL,
              result.dataObjectVersionToExport(),
              result.transferWithLogBookLFC(),
              result.transferRequestParameters().toRequestParameters(),
              result.sedaVersion());

      Path tmpDipPath = Workspace.createTempFile(operation);
      Context context =
          new Context(operation, tenantDb, accessContract, ontologyMapper, null, tmpDipPath);
      doExport(context, exportConfig, archiveUnits, storageDao);

      return new TransferPaths(tmpAusPath, tmpDipPath);

    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private void checkMaxSize(List<ArchiveUnit> units, DataObjectVersionToExport dov) {
    if (dov != null && dov.dataObjectVersions() != null && maxSize > 0) {
      long size = 0;

      for (ArchiveUnit unit : units) {
        for (BinaryQualifier bq : dov.dataObjectVersions()) {
          String qualifier = bq.toString();
          for (Qualifiers q : unit.getQualifiers()) {
            if (q.getQualifier().equals(qualifier)) {
              ObjectVersion objectVersion = ObjectVersion.getGreatestVersion(q.getVersions());
              size += objectVersion.getSize();
              if (size > maxSize) {
                throw new BadRequestException(
                    String.format(
                        "The export dip '%s' is greater than the allowed max size of '%s'",
                        size, maxSize));
              }
              break;
            }
          }
        }
      }
    }
  }

  private TransferResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    TransferQuery transferQuery = ArchiveUnitQueryFactory.createTransferQuery(query);
    TransferParser parser = TransferParser.create(tenant, accessContract, ontologyMapper);

    try {
      // TODO Deals with maxSizeThreshold
      SearchRequest request = parser.createRequest(transferQuery.searchQuery());

      // Refreshing an index is usually not recommended. We could have indexed all
      // documents with refresh=wait_for|true property. Unfortunately this is very costly
      // in case of mass ingest/update.
      searchService.refresh();

      log.info("Update JSON  - request: {}", JsonUtils.toJson(request));
      SearchResponse<ArchiveUnit> response = searchEngineService.search(request, ArchiveUnit.class);

      // Refreshing an index is usually not recommended. We could have indexed all
      // documents with refresh=wait_for|true property. Unfortunately this is very costly
      // in case of mass ingest/update.
      searchService.refresh();

      // TODO check if detail overflows
      List<ArchiveUnit> units = response.hits().hits().stream().map(Hit::source).toList();
      return new TransferResult<>(
          units,
          transferQuery.dataObjectVersionToExport(),
          transferQuery.transferWithLogBookLFC(),
          transferQuery.transferRequestParameters(),
          transferQuery.sedaVersion(),
          maxSize);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", transferQuery), ex);
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
              OperationType.TRANSFER_ARCHIVE,
              created,
              revertPatch));

      unit.buildProperties(ontologyMapper);
      sequenceWriter.write(unit);
    }
  }

  public void commit(OperationDb operation, TenantDb tenantDb, TransferPaths paths) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    List<StorageObject> storageObjects = new ArrayList<>();

    // Write Archive Units (.aus) to temporary file
    storageObjects.add(
        new PathStorageObject(paths.ausPath, operationId, StorageObjectType.aus, true));
    storageObjects.add(
        new PathStorageObject(paths.dipPath, operationId, StorageObjectType.dip, true));

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

  public void store(OperationDb operation, TenantDb tenantDb, TransferPaths paths) {
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = Files.newInputStream(paths.ausPath)) {
      doStore(operation, tenantDb, ausStream, storageDao);
    } catch (IOException ex) {
      NioUtils.deleteDirQuietly(paths.ausPath);
      NioUtils.deleteDirQuietly(paths.dipPath);
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
        new ArchiveReporter(ReportType.TRANSFER, ReportStatus.OK, operation, reportPath)) {

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
        // TODO Optimization refresh only if Management has changed
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

    // Write transfer report to offer
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
      log.error(ExceptionsUtils.format("Transfer archive units failed", e, operation), ex);
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
      log.error(ExceptionsUtils.format("Transfer archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Failed to index archive units. Waiting for automatic retry.");
      operationService.save(operation);
    }
    // TODO : purge aus files
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

  private void doExport(
      Context context, ExportConfig exportConfig, List<ArchiveUnit> units, StorageDao storageDao)
      throws IOException {

    // Add the archive unit to its parent archive unit (if any)
    Map<Long, ArchiveUnit> unitMap = UnitUtils.mapById(units);

    List<ArchiveUnit> srcUnits = new ArrayList<>();

    for (ArchiveUnit unit : units) {
      ArchiveUnit parentUnit = unitMap.get(unit.getParentId());
      if (parentUnit == null) {
        srcUnits.add(unit);
      } else {
        parentUnit.getChildUnitMap().put(unit.getId().toString(), unit);
      }
    }

    Long tenantId = context.tenantDb().getId();
    ArrayList<String> offers = context.tenantDb().getStorageOffers();
    Long operationId = context.operationDb().getId();
    Exporter exporter = new Sedav2Exporter(tenantId, operationId, offers, storageDao, exportConfig);
    String version =
        StringUtils.isBlank(exportConfig.sedaVersion())
            ? "2.2"
            : exportConfig.sedaVersion().toUpperCase();

    switch (version) {
      case "2.2", "V2.2", "SIP_2.2", "SIP_V2.2" -> exporter.exportSip(srcUnits, context.path());
      case "2.1", "V2.1", "SIP_2.1", "SIP_V2.1" -> exporter.exportSip(srcUnits, context.path());
      case "DIP_2.2", "DIP_V2.2" -> exporter.exportDip(srcUnits, context.path());
      case "DIP_2.1", "DIP_V2.1" -> exporter.exportDip(srcUnits, context.path());
      default -> throw new BadRequestException(
          String.format("Export '%s' is not implemented", version));
    }
  }
}
