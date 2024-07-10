/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import static fr.xelians.esafe.common.utils.Hash.MD5;
import static fr.xelians.esafe.operation.domain.ActionType.DELETE;
import static fr.xelians.esafe.operation.domain.ActionType.UPDATE;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SequenceWriter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationParser;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationQuery;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationRequest;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationResult;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.domain.unit.rules.computed.AppraisalComputedRules;
import fr.xelians.esafe.archive.domain.unit.rules.computed.HoldComputedRules;
import fr.xelians.esafe.archive.task.EliminationTask;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
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
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class EliminationService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  private static final byte[] ZERO_BYTES = new byte[] {0};

  private final ProcessingService processingService;
  private final SearchService searchService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final LogbookService logbookService;
  private final SearchEngineService searchEngineService;
  private final AccessContractService accessContractService;
  private final OntologyService ontologyService;

  public Long eliminate(
      Long tenant,
      String accessContract,
      EliminationQuery eliminationQuery,
      String user,
      String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(eliminationQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(eliminationQuery);
    OperationDb operation =
        OperationFactory.eliminateArchiveOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new EliminationTask(operation, this));
    return operation.getId();
  }

  public Path check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();
    Map<String, RuleDb> ruleMap = new HashMap<>();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // Query
      String query = operation.getProperty02();
      // TODO  Optimisation: only retrieve relevant fields of archive unit
      EliminationResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();

      Path tmpAusPath = Workspace.createTempFile(operation);
      List<String> offers = tenantDb.getStorageOffers();
      LocalDate today = LocalDate.now();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();

      // Group selected Archive Units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      // Eliminate top archive units by operation id
      for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
        Long opId = entry.getKey();
        List<ArchiveUnit> indexedUnits = entry.getValue();

        // Read from storage offers archive units with unit.operationId and group them by archive
        // unit id. Check storage and index coherency. Could be optional
        Map<Long, ArchiveUnit> storedUnitMap =
            UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

        // Loop on indexed archive units
        for (ArchiveUnit indexedUnit : indexedUnits) {
          Long archiveUnitId = indexedUnit.getId();

          // Check storage and index coherency. Could be optional
          ArchiveUnit storedUnit = storedUnitMap.get(archiveUnitId);
          UnitUtils.checkVersion(indexedUnit, storedUnit);

          eliminateChild(indexedUnit, today);
          archiveUnits.add(indexedUnit);
        }
      }

      Context context =
          new Context(operation, tenantDb, accessContract, ontologyMapper, ruleMap, tmpAusPath);
      doElimination(context, archiveUnits, today, storageDao);
      return tmpAusPath;
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

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

  public void storeOperation(OperationDb operation) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = storageDao.getAusStream(tenant, offers, operationId)) {
      doStore(operation, tenantDb, ausStream, storageDao);
      logbookService.index(operation);
      operationService.unlockAndSave(operation, OperationStatus.OK, "Index done successfully");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(ExceptionsUtils.format("Eliminate archive units failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_STORE);
      operation.setMessage("Failed to eliminate archive unit. Waiting for automatic retry.");
      operationService.save(operation);
      return;
    }

    // TODO : purge aus files 'Not here. Delete the .aus after indexing)
    // TODO : add information  in the operation about the number of deletion for the access register
    // (registre des fonds)
    // be careful to be idempotent!
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

  private EliminationResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    EliminationQuery eliminationQuery = ArchiveUnitQueryFactory.createEliminationQuery(query);
    EliminationParser eliminationParser =
        EliminationParser.create(tenant, accessContract, ontologyMapper);

    try {
      EliminationRequest eliminationRequest = eliminationParser.createRequest(eliminationQuery);

      log.info("Update JSON  - request: {}", JsonUtils.toJson(eliminationRequest.searchRequest()));

      // Refreshing an index is usually not recommended. We could have indexed all
      // documents with refresh=wait_for|true property. Unfortunately this is very costly
      // in case of mass ingest/update.
      searchService.refresh();

      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(eliminationRequest.searchRequest(), ArchiveUnit.class);

      // TODO check if detail overflows
      List<ArchiveUnit> nodes = response.hits().hits().stream().map(Hit::source).toList();
      return new EliminationResult<>(nodes);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed",
          String.format("Failed to parse query '%s'", eliminationQuery),
          ex);
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
    try (OutputStream os = Files.newOutputStream(reportPath);
        JsonGenerator generator = JsonService.createGenerator(os)) {

      generator.writeStartObject();
      generator.writeStringField("Type", ReportType.ELIMINATION.toString());
      generator.writeStringField("Date", LocalDateTime.now().toString());
      generator.writeNumberField("Tenant", tenant);
      generator.writeNumberField("OperationId", operationId);
      generator.writeStringField("GrantDate", operation.getCreated().toString());
      generator.writeStringField("Status", ReportStatus.OK.toString());

      int numOfUnits = 0;
      int numOfObjectGroups = 0;
      int numOfPhysicalObjects = 0;
      int numOfBinaryObjects = 0;
      long sizeOfBinaryObjects = 0;

      generator.writeFieldName("ArchiveUnits");
      generator.writeStartArray();

      // Get updated Archive Units from stream
      // TODO Optimize by getting only relevant fields from archive unit
      Iterator<ArchiveUnit> iterator = JsonService.toArchiveUnitIterator(ausStream);
      Iterator<List<ArchiveUnit>> listIterator = ListIterator.iterator(iterator, 10000);
      while (listIterator.hasNext()) {
        List<ArchiveUnit> indexedArchiveUnits = listIterator.next();
        Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(indexedArchiveUnits);

        // Write archive units by operation id
        for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
          Long opId = entry.getKey();
          List<ArchiveUnit> archiveUnits = entry.getValue();

          for (ArchiveUnit au : archiveUnits) {
            numOfUnits++;

            generator.writeStartObject();
            generator.writeStringField("SystemId", au.getId().toString());
            generator.writeNumberField("OperationId", au.getOperationId());
            generator.writeStringField("ArchivalAgencyIdentifier", au.getServiceProducer());
            generator.writeStringField("CreationDate", au.getCreationDate().toString());

            boolean isNotEmpty = false;

            generator.writeFieldName("BinaryDataObjects");
            generator.writeStartArray();
            for (Qualifiers qualifiers : au.getQualifiers()) {
              if (qualifiers.isBinaryQualifier()) {
                for (ObjectVersion ov : qualifiers.getVersions()) {
                  isNotEmpty = true;
                  numOfBinaryObjects++;
                  sizeOfBinaryObjects += ov.getSize();
                  generator.writeStartObject();
                  generator.writeStringField("DataObjectSystemId", ov.getId().toString());
                  generator.writeStringField("DataObjectVersion", ov.getDataObjectVersion());
                  generator.writeNumberField("Size", ov.getSize());
                  generator.writeStringField("DigestAlgorithm", ov.getAlgorithm());
                  generator.writeStringField("MessageDigest", ov.getMessageDigest());
                  generator.writeEndObject();
                }
              }
            }
            generator.writeEndArray();

            generator.writeFieldName("PhysicalDataObjects");
            generator.writeStartArray();
            for (Qualifiers qualifiers : au.getQualifiers()) {
              if (qualifiers.isPhysicalQualifier()) {
                for (ObjectVersion ov : qualifiers.getVersions()) {
                  isNotEmpty = true;
                  numOfPhysicalObjects++;
                  generator.writeStartObject();
                  generator.writeStringField("DataObjectSystemId", ov.getId().toString());
                  generator.writeStringField("DataObjectVersion", ov.getDataObjectVersion());
                  generator.writeEndObject();
                }
              }
            }

            if (isNotEmpty) {
              numOfObjectGroups++;
            }

            generator.writeEndArray();
            generator.writeEndObject();
          }

          // Read from storage offers archive units with unit.operationId and group them by archive
          // unit id
          Map<Long, ArchiveUnit> storedUnits =
              getStoredUnits(opId, tenant, offers, archiveUnits, storageDao);

          if (storedUnits.isEmpty()) {
            deleteObjects(operation, offers, tenant, opId);
          } else {
            byte[] bytes = JsonService.collToBytes(storedUnits.values(), JsonConfig.DEFAULT);
            storageObjects.add(new ByteStorageObject(bytes, opId, StorageObjectType.uni));
            byteCount += bytes.length;

            if (byteCount > 256_000_000) {
              // Commit the created/modified units to offers and create actions
              storageDao
                  .putStorageObjects(tenant, offers, storageObjects)
                  .forEach(e -> operation.addAction(StorageAction.create(UPDATE, e)));
              storageObjects = new ArrayList<>();
              byteCount = 0;
            }
          }
        }

        // Commit the created/modified units to offers and create actions
        if (!storageObjects.isEmpty()) {
          storageDao
              .putStorageObjects(tenant, offers, storageObjects)
              .forEach(e -> operation.addAction(StorageAction.create(UPDATE, e)));
        }

        // Index Archive units in Search Engine (properties are already built in each archive)
        if (isFirst && !listIterator.hasNext()) {
          searchService.bulkDeleteRefresh(
              indexedArchiveUnits.stream().map(ArchiveUnit::getId).toList());
        } else {
          searchService.bulkDelete(indexedArchiveUnits.stream().map(ArchiveUnit::getId).toList());
          needRefresh = true;
        }
        isFirst = false;
      }

      if (needRefresh) {
        searchService.refresh();
      }

      generator.writeEndArray();

      generator.writeNumberField("NumOfUnits", numOfUnits);
      generator.writeNumberField("NumOfObjectGroups", numOfObjectGroups);
      generator.writeNumberField("NumOfPhysicalObjects", numOfPhysicalObjects);
      generator.writeNumberField("NumOfBinaryObjects", numOfBinaryObjects);
      generator.writeNumberField("SizeOfBinaryObjects", sizeOfBinaryObjects);

      generator.writeEndObject();
    }

    // Write delete report to offer
    List<StorageObject> psois =
        List.of(new PathStorageObject(reportPath, operationId, StorageObjectType.rep));
    storageDao
        .putStorageObjects(tenant, offers, psois)
        .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));
  }

  private Map<Long, ArchiveUnit> getStoredUnits(
      Long opId,
      Long tenant,
      List<String> offers,
      List<ArchiveUnit> archiveUnits,
      StorageDao storageDao)
      throws IOException {

    try {
      List<ArchiveUnit> units = storageDao.getArchiveUnits(tenant, offers, opId);
      Map<Long, ArchiveUnit> storedUnits = UnitUtils.mapById(units);

      // Loop on eliminated archive units
      for (ArchiveUnit archiveUnit : archiveUnits) {
        Long archiveUnitId = archiveUnit.getId();
        // Remove the stored archive
        storedUnits.remove(archiveUnitId);
      }
      return storedUnits;
    } catch (NotFoundException ex) {
      return new HashMap<>();
    }
  }

  // TODO: delete objects after a while via a batch
  // Note. the corresponding atr object must not be deleted.
  private void deleteObjects(OperationDb operation, List<String> offers, Long tenant, Long opId)
      throws IOException {
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.uni);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.uni, MD5, ZERO_BYTES));
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.bin);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.bin, MD5, ZERO_BYTES));
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.mft);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.mft, MD5, ZERO_BYTES));
  }

  private void doElimination(
      Context context, List<ArchiveUnit> parentUnits, LocalDate today, StorageDao storageDao)
      throws IOException {
    Long tenant = context.tenantDb().getId();

    // Eliminate children archive units by operation id
    Map<Long, ArchiveUnit> topUnitMap = UnitUtils.mapById(parentUnits);

    String pitId = null;
    try (SequenceWriter writer = JsonService.createSequenceWriter(context.path())) {
      writer.writeAll(parentUnits);
      pitId = searchService.openPointInTime();

      // We limit the search to 1000 terms. Elastic search limit is 65,536
      for (List<ArchiveUnit> partList : ListUtils.partition(parentUnits, 1000)) {
        List<Long> parentIds = partList.stream().map(ArchiveUnit::getId).toList();
        // TODO  optimisation: only retrieve relevant fields of archive unit and process with
        // greater chunks
        Stream<ArchiveUnit> stream =
            searchService
                .searchAllChildrenStream(tenant, context.accessContractDb(), parentIds, pitId)
                .filter(au -> !topUnitMap.containsKey(au.getId()));
        CollUtils.chunk(stream, 10000)
            .forEach(
                childUnits -> eliminateChildren(context, childUnits, today, storageDao, writer));
      }
    } finally {
      searchService.closePointInTime(pitId);
    }
  }

  @SneakyThrows
  private void eliminateChildren(
      Context context,
      List<ArchiveUnit> childUnits,
      LocalDate today,
      StorageDao storageDao,
      SequenceWriter writer) {
    Long tenant = context.tenantDb().getId();
    List<String> offers = context.tenantDb().getStorageOffers();

    // Group Archive Units by operation id
    Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(childUnits);

    // Eliminate archive units by operation id
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

        // Process each child
        eliminateChild(childUnit, today);

        // TODO  optimisation: write only relevant fields
        writer.write(childUnit);
      }
    }
  }

  private static void eliminateChild(ArchiveUnit childUnit, LocalDate today) {
    checkAppraisalRule(childUnit, today);
    checkHoldRule(childUnit, today);
  }

  private static void checkAppraisalRule(ArchiveUnit childUnit, LocalDate today) {
    AppraisalComputedRules rule = childUnit.getComputedInheritedRules().getAppraisalComputedRules();
    if (rule == null) {
      throw new BadRequestException(
          String.format(
              "Cannot eliminate '%s' archive unit without appraisal rule", childUnit.getId()));
    }

    if (!"Destroy".equalsIgnoreCase(rule.getFinalAction())) {
      throw new BadRequestException(
          String.format(
              "Cannot eliminate '%s' archive unit with appraisal final action %s",
              childUnit.getId(), rule.getFinalAction()));
    }

    LocalDate maxEndDate = rule.getMaxEndDate();
    if (maxEndDate == null || !maxEndDate.isBefore(today)) {
      throw new BadRequestException(
          String.format(
              "Cannot eliminate '%s' archive unit with '%s' appraisal max end date ",
              childUnit.getId(), Objects.toString(rule.getMaxEndDate(), "undefined")));
    }
  }

  private static void checkHoldRule(ArchiveUnit childUnit, LocalDate today) {
    HoldComputedRules rule = childUnit.getComputedInheritedRules().getHoldComputedRules();
    if (rule != null) {
      LocalDate maxEndDate = rule.getMaxEndDate();
      if (maxEndDate == null || !maxEndDate.isBefore(today)) {
        throw new BadRequestException(
            String.format(
                "Cannot eliminate '%s' archive unit with '%s' hold max end date",
                childUnit.getId(), Objects.toString(rule.getMaxEndDate(), "undefined")));
      }
    }
  }
}
