/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import fr.xelians.esafe.admin.domain.report.*;
import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.probative.ProbativeLbkIterator;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.atr.BinaryDataObjectReply;
import fr.xelians.esafe.archive.domain.atr.DataObjectGroupReply;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.probativevalue.AtrBinaryObject;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueParser;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueQuery;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueResult;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.task.ProbativeValueTask;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.*;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class ProbativeValueService {

  public static final String OPERATION_BEGIN = "BEGIN";
  public static final String OPERATION_COMMIT = "COMMIT";
  public static final String ACTION_CREATE = "CREATE";
  public static final String ACTION_DELETE = "DELETE";
  public static final String ACTION_UPDATE = "UPDATE";

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";

  public static final String CHECK_PROBATIVE_FAILED = "Check probative value failed";

  private static final Hash HASH = Hash.VALUES[0];
  public static final String ATR = "_atr_";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final StorageService storageService;
  private final SearchEngineService searchEngineService;
  private final AccessContractService accessContractService;
  private final OntologyService ontologyService;
  private final LogbookService logbookService;
  private final TenantService tenantService;

  // TODO implement a nightly log coherency batch on each offer
  // TODO Check on the last log coherency batch is ok (a faire)
  // TODO Add a logbook secure date
  // TODO Compare the storage uni checksum with the logbook uni checksum

  public Long probativeValue(
      Long tenant,
      String accessContract,
      ProbativeValueQuery probativeValueQuery,
      String user,
      String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(probativeValueQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(probativeValueQuery);
    OperationDb operation =
        OperationFactory.probativeValueOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new ProbativeValueTask(operation, this));
    return operation.getId();
  }

  public Path check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    // Search Archive Units to probe
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      // Query
      String query = operation.getProperty02();
      ProbativeValueResult<ArchiveUnit> result =
          search(tenant, accessContract, ontologyMapper, query);

      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();
      List<ArchiveUnit> selectedUnits = result.results();

      // Group selected Archive Units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      // Export archive units by operation id
      for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
        Long opId = entry.getKey();
        List<ArchiveUnit> indexedUnits = entry.getValue();

        // From storage offers read units with operationId and map by unit id.
        Map<Long, ArchiveUnit> storedUnitMap =
            UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

        // Loop on indexed archive units
        for (ArchiveUnit indexedUnit : indexedUnits) {
          Long archiveUnitId = indexedUnit.getId();

          // Check storage and index coherency
          ArchiveUnit storedUnit = storedUnitMap.get(archiveUnitId);
          UnitUtils.checkVersion(indexedUnit, storedUnit);
          archiveUnits.add(indexedUnit);
        }
      }

      // Check all probative values
      Path reportPath = Workspace.createTempFile(operation);
      Context context =
          new Context(operation, tenantDb, accessContract, ontologyMapper, null, reportPath);
      checkProbativeValue(context, result.usages(), result.version(), archiveUnits, storageDao);
      return reportPath;

    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  public void commit(OperationDb operation, TenantDb tenantDb, Path reportPath) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    // Write probative report to offer
    List<StorageObject> storageObjects =
        List.of(new PathStorageObject(reportPath, operationId, StorageObjectType.rep));

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      storageDao
          .putStorageObjects(tenant, offers, storageObjects)
          .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));
    } catch (IOException ex) {
      // Rollback as much as possible
      storageService.deleteObjectsQuietly(offers, tenant, storageObjects);
      throw new InternalException(ex);
    }
  }

  private void checkProbativeValue(
      Context context,
      Set<BinaryQualifier> usages,
      String probativeVersion,
      List<ArchiveUnit> units,
      StorageDao storageDao)
      throws IOException {

    Integer version = parseVersion(probativeVersion);

    OperationDb operationDb = context.operationDb();
    TenantDb tenantDb = context.tenantDb();

    Map<String, byte[]> storageAtrMap = new HashMap<>();
    Map<Long, Set<Long>> lbkOperationsMap = new HashMap<>();

    ProbativeValueReport report = new ProbativeValueReport();
    report.setDate(LocalDateTime.now());
    report.setTenant(tenantDb.getId());
    report.setOperationId(operationDb.getId());
    report.setType(ReportType.PROBATIVE_VALUE);
    report.setStatus(ReportStatus.OK);

    // Compare the storage binary object checksum with binary object checksum from uni and atr
    checkBinaryObjectChecksums(
        report, tenantDb, units, usages, version, storageDao, storageAtrMap, lbkOperationsMap);

    // Compare the storage atr checksum with logbook atr checksum
    checkAtrChecksum(report, tenantDb, storageAtrMap, lbkOperationsMap);

    JsonService.write(report, context.path(), JsonConfig.DEFAULT);
  }

  private void checkBinaryObjectChecksums(
      ProbativeValueReport report,
      TenantDb tenantDb,
      List<ArchiveUnit> units,
      Set<BinaryQualifier> usages,
      Integer version,
      StorageDao storageDao,
      Map<String, byte[]> storageAtrMap,
      Map<Long, Set<Long>> lbkOperationsMap)
      throws IOException {

    Map<Long, AtrBinaryObject> bdorMap = new HashMap<>();

    for (ArchiveUnit unit : units) {

      List<Qualifiers> qualifiers = unit.getQualifiers();

      for (BinaryQualifier usage : usages) {
        BinaryVersion binaryVersion = new BinaryVersion(usage, version);
        ObjectVersion ov = Qualifiers.getObjectVersion(qualifiers, binaryVersion);
        if (ov == null) {
          throw new BadRequestException(
              CHECK_PROBATIVE_FAILED,
              String.format("Binary object with version '%s' does not exist", binaryVersion));
        }

        BinaryObjectDetail boDetail = new BinaryObjectDetail();
        report.getBinaryObjectDetails().add(boDetail);
        boDetail.setBinaryObjectId(ov.getId());
        boDetail.setDataObjectVersion(ov.getDataObjectVersion());
        boDetail.setAlgorithm(HashUtils.getHash(ov.getAlgorithm()));
        boDetail.setBinaryObjectSize(ov.getSize());
        boDetail.setOperationId(ov.getOperationId());
        boDetail.setStatus(ReportStatus.OK);

        // Compare the computed binary object and the stored binary object digest from archive unit
        byte[] boChecksum = getChecksum(report, boDetail, storageDao, tenantDb, ov);
        checkBinaryObjectChecksum(report, boDetail, boChecksum, ov);

        // Get operationId (many ov can refer to the same operation)
        Long operationId = ov.getOperationId();

        // Update maps if operation was not already processed
        if (!storageAtrMap.containsKey(operationId + ATR + HASH)) {
          byte[] atrBytes = getAtr(report, boDetail, storageDao, tenantDb, operationId);
          updateMaps(atrBytes, operationId, storageAtrMap, bdorMap);

          // Get secure number (ie. the logbook operation id) from operation
          // Note. many operations can refer to the same secure number
          Long lbkId = getSecureNumber(tenantDb, operationId);

          Set<Long> ops = lbkOperationsMap.computeIfAbsent(lbkId, l -> new HashSet<>());
          ops.add(operationId);
        }

        // Compare the binary object digest from atr with archive unit version
        checkBinaryObjectChecksum(report, boDetail, bdorMap, ov);
      }
    }
  }

  private static Integer parseVersion(String version) {
    if ("LAST".equalsIgnoreCase(version)) {
      return null;
    }
    if (Utils.isPositiveInteger(version)) {
      return Integer.parseInt(version);
    }
    throw new BadRequestException(
        CHECK_PROBATIVE_FAILED, String.format("Version '%s' is not valid", version));
  }

  private static void checkBinaryObjectChecksum(
      ProbativeValueReport report,
      BinaryObjectDetail boDetail,
      byte[] boChecksum,
      ObjectVersion ov) {

    String boDigest = HashUtils.encodeHex(boChecksum);
    boDetail.setStorageBinaryObjectChecksum(boDigest);
    boDetail.setUnitBinaryObjectChecksum(ov.getMessageDigest());

    if (!boDigest.equals(ov.getMessageDigest())) {
      report.setStatus(ReportStatus.KO);
      boDetail.setStatus(ReportStatus.KO);
      boDetail.setStatusDetail("Unit and storage checksums are not equal");
    }
  }

  private static void updateMaps(
      byte[] atrBytes,
      Long operationId,
      Map<String, byte[]> storageAtrMap,
      Map<Long, AtrBinaryObject> bdorMap)
      throws IOException {

    // We compute all hashes in order to avoid reloading the ATR object from offer
    Arrays.stream(Hash.VALUES)
        .forEach(
            hash ->
                storageAtrMap.put(operationId + ATR + hash, HashUtils.checksum(hash, atrBytes)));

    ArchiveTransferReply atr = JsonService.toArchiveTransferReply(atrBytes);
    for (DataObjectGroupReply dogReply : atr.getDataObjectGroupReplys()) {
      for (BinaryDataObjectReply bdoReply : dogReply.getBinaryDataObjectReplys()) {
        bdorMap.put(bdoReply.getSystemId(), new AtrBinaryObject(atr.getGrantDate(), bdoReply));
      }
    }
  }

  private static void checkBinaryObjectChecksum(
      ProbativeValueReport report,
      BinaryObjectDetail detail,
      Map<Long, AtrBinaryObject> bdorMap,
      ObjectVersion ov) {

    AtrBinaryObject abdo = bdorMap.get(ov.getId());
    if (abdo == null) {
      report.setStatus(ReportStatus.KO);
      detail.setStatus(ReportStatus.KO);
      detail.setStatusDetail("Failed to get Atr checksum for binary object");
      return;
    }

    BinaryDataObjectReply bdoReply = abdo.bdoReply();
    detail.setAtrBinaryObjectChecksum(bdoReply.getMessageDigest());
    detail.setGrantDate(abdo.grantDate());

    if (!ov.getDataObjectVersion().equals(bdoReply.getVersion())) {
      report.setStatus(ReportStatus.KO);
      detail.setStatus(ReportStatus.KO);
      detail.setStatusDetail(
          String.format(
              "Storage and Atr binary object version are not equal: '%s' - '%s'",
              ov.getDataObjectVersion(), bdoReply.getVersion()));
      return;
    }

    Hash ovAlgorithm = HashUtils.getHash(ov.getAlgorithm());
    Hash bdoAlgorithm = HashUtils.getHash(bdoReply.getDigestAlgorithm());
    if (!ovAlgorithm.equals(bdoAlgorithm)) {
      report.setStatus(ReportStatus.KO);
      detail.setStatus(ReportStatus.KO);
      detail.setStatusDetail(
          String.format(
              "Storage and Atr binary object algorithms are not equal : '%s' - '%s'",
              ovAlgorithm, bdoAlgorithm));
      return;
    }

    if (!ov.getMessageDigest().equals(bdoReply.getMessageDigest())) {
      report.setStatus(ReportStatus.KO);
      detail.setStatus(ReportStatus.KO);
      detail.setStatusDetail("Storage and Atr binary object checksums are not equal");
    }
  }

  private static byte[] getAtr(
      ProbativeValueReport report,
      BinaryObjectDetail detail,
      StorageDao storageDao,
      TenantDb tenantDb,
      Long operationId)
      throws IOException {

    byte[] data = null;
    byte[] checksum = null;
    List<String> offers = tenantDb.getStorageOffers();

    for (String offer : offers) {
      try (InputStream is =
          storageDao.getAtrStream(tenantDb.getId(), List.of(offer), operationId)) {

        data = IOUtils.toByteArray(is);
        byte[] cs = HashUtils.checksum(Hash.SHA512, data);
        if (checksum == null) {
          checksum = cs;
        } else if (!Arrays.equals(checksum, cs)) {
          report.setStatus(ReportStatus.KO);
          detail.setStatus(ReportStatus.KO);
          detail.setStatusDetail("Atr checksums are not equal on all offers");
          return data;
        }
      }
    }
    return data;
  }

  // get checksum from storage offer
  private static byte[] getChecksum(
      ProbativeValueReport report,
      BinaryObjectDetail boDetail,
      StorageDao storageDao,
      TenantDb tenantDb,
      ObjectVersion ov)
      throws IOException {

    byte[] checksum = null;
    Hash hash = HashUtils.getHash(ov.getAlgorithm());
    List<String> offers = tenantDb.getStorageOffers();

    for (String offer : offers) {
      try (InputStream is =
          storageDao.getBinaryObjectStream(
              tenantDb.getId(), List.of(offer), ov.getOperationId(), ov.getPos(), ov.getId())) {

        byte[] cs = HashUtils.checksum(hash, is);
        if (checksum == null) {
          checksum = cs;
        } else if (!Arrays.equals(checksum, cs)) {
          report.setStatus(ReportStatus.KO);
          boDetail.setStatus(ReportStatus.KO);
          boDetail.setStatusDetail("Checksums are not equal on all offers");
          return cs;
        }
      }
    }
    return checksum;
  }

  // Get ATR digest from logbook and compare ATR digest from logbook with ATR Map checksum
  private void checkAtrChecksum(
      ProbativeValueReport report,
      TenantDb tenantDb,
      Map<String, byte[]> storageAtrMap,
      Map<Long, Set<Long>> lbkOperationsMap)
      throws IOException {

    for (Map.Entry<Long, Set<Long>> entry : lbkOperationsMap.entrySet()) {
      Long lbkId = entry.getKey();
      Map<String, byte[]> lbkAtrMap = getLbkAtrMap(tenantDb, lbkId);
      for (Long operationId : entry.getValue()) {
        checkAtrChecksum(report, operationId, lbkAtrMap, storageAtrMap);
      }
    }
  }

  private void checkAtrChecksum(
      ProbativeValueReport report,
      Long operationId,
      Map<String, byte[]> lbkAtrMap,
      Map<String, byte[]> storageAtrMap) {

    AtrDetail detail = new AtrDetail();
    report.getAtrDetails().add(detail);
    detail.setOperationId(operationId);
    detail.setStatus(ReportStatus.OK);

    for (Hash hash : Hash.VALUES) {
      String key = operationId + ATR + hash;
      byte[] lbkAtrChecksum = lbkAtrMap.get(key);
      if (lbkAtrChecksum != null) {
        detail.setAlgorithm(hash);
        detail.setLbkAtrChecksum(HashUtils.encodeHex(lbkAtrChecksum));

        byte[] storageAtrChecksum = storageAtrMap.get(key);
        if (storageAtrChecksum == null) {
          report.setStatus(ReportStatus.KO);
          detail.setStatus(ReportStatus.KO);
          detail.setStatusDetail("Storage hash does not exist on storage");
          return;
        }

        detail.setStorageAtrChecksum(HashUtils.encodeHex(storageAtrChecksum));
        if (!Arrays.equals(storageAtrChecksum, lbkAtrChecksum)) {
          report.setStatus(ReportStatus.KO);
          detail.setStatus(ReportStatus.KO);
          detail.setStatusDetail("Storage and logbook Atr checksums are not equal");
        }

        return;
      }
    }

    report.setStatus(ReportStatus.KO);
    detail.setStatus(ReportStatus.KO);
    detail.setStatusDetail("Atr does not exist in Logbook");
  }

  private Map<String, byte[]> getLbkAtrMap(TenantDb tenantDb, Long lbkId) throws IOException {
    Map<String, byte[]> lbkAtrMap = new HashMap<>();

    try (LbkIterator it = new ProbativeLbkIterator(tenantDb, storageService, lbkId)) {
      while (it.hasNext()) {
        LogbookOperation operation = it.next();
        if (operation.getType() == OperationType.INGEST_ARCHIVE) {
          for (StorageAction storageAction : operation.getStorageActions()) {
            if (storageAction.getType() == StorageObjectType.atr) {
              String key = operation.getId() + ATR + storageAction.getHash();
              byte[] lbkAtrChecksum = storageAction.getChecksum();
              lbkAtrMap.put(key, lbkAtrChecksum);
            }
          }
        }
      }
    }

    return lbkAtrMap;
  }

  private Long getSecureNumber(TenantDb tenantDb, Long operationId) throws IOException {
    LogbookOperation logbookOperation =
        logbookService.getOperationSe(tenantDb.getId(), operationId);
    Long secureNumber = logbookOperation.getSecureNumber();
    if (secureNumber == null) {
      throw new BadRequestException(
          CHECK_PROBATIVE_FAILED,
          String.format("Operation '%s' is not (yet) secured", operationId));
    }
    return secureNumber;
  }

  private ProbativeValueResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    ProbativeValueQuery probativeQuery = ArchiveUnitQueryFactory.createProbativeValueQuery(query);
    ProbativeValueParser probativeParser =
        ProbativeValueParser.create(tenant, accessContract, ontologyMapper);

    try {
      SearchRequest exportRequest = probativeParser.createRequest(probativeQuery.searchQuery());

      log.info("Update JSON  - request: {}", JsonUtils.toJson(exportRequest));
      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(exportRequest, ArchiveUnit.class);

      // TODO check if detail overflows
      List<ArchiveUnit> units = response.hits().hits().stream().map(Hit::source).toList();
      return new ProbativeValueResult<>(units, probativeQuery.usages(), probativeQuery.version());

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", probativeQuery), ex);
    }
  }
}
