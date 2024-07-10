/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import fr.xelians.esafe.antivirus.AntiVirus;
import fr.xelians.esafe.antivirus.AntiVirusScanner;
import fr.xelians.esafe.antivirus.ScanResult;
import fr.xelians.esafe.antivirus.ScanStatus;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.ingest.AbstractManifestParser;
import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.archive.domain.ingest.sedav2.*;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.DataObjectGroup;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.task.IngestionTask;
import fr.xelians.esafe.common.constant.Env;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.ForbiddenException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.ZipUtils;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.*;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.referential.entity.IngestContractDb;
import fr.xelians.esafe.referential.service.ReferentialService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.pack.Packer;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class IngestService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "id must be not null";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final StorageService storageService;
  private final TenantService tenantService;
  private final SearchService searchService;
  private final LogbookService logbookService;
  private final DateRuleService dateRuleService;
  private final ReferentialService referentialService;
  private final AntiVirusScanner antiVirusScanner;

  public Long ingestStream(
      Long tenant, ContextId contextId, InputStream inputStream, String user, String app)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(contextId, "contextId must be not null");
    Assert.notNull(inputStream, "inputStream must be not null");
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    // Create operation
    OperationDb operation = OperationFactory.ingestArchiveOp(tenant, contextId, user, app);
    operation = operationService.save(operation);

    // Create temporary dir
    Path ws = Workspace.getPath(operation);
    if (Files.exists(ws)) {
      throw new FileAlreadyExistsException(ws.toString());
    }
    Files.createDirectories(ws);

    // Create path with operation id
    Path sipPath = getSipPath(operation);

    try {
      Files.copy(inputStream, sipPath);
    } catch (Exception ex) {
      // We do not need to keep this operation because we throw an InternalException
      operationService.deleteOperations(operation.getId());
      Files.deleteIfExists(sipPath);
      throw new InternalException(
          "Failed to ingest archive",
          String.format("Failed to create ingest path: '%s'", sipPath),
          ex);
    }

    // Create and submit task
    processingService.submit(new IngestionTask(operation, this));
    return operation.getId();
  }

  public Long ingestMultipartFile(
      Long tenant, ContextId contextId, MultipartFile multipartFile, String user, String app)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(contextId, "contextId must be not null");
    Assert.notNull(multipartFile, "multipart file must be not null");
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    OperationDb operation = OperationFactory.ingestArchiveOp(tenant, contextId, user, app);
    operation = operationService.save(operation);

    // Create temporary dir
    Path ws = Workspace.getPath(operation);
    if (Files.exists(ws)) {
      throw new FileAlreadyExistsException(ws.toString());
    }
    Files.createDirectories(ws);

    // Create path with operation id
    Path sipPath = getSipPath(operation);

    try {
      multipartFile.transferTo(sipPath);
    } catch (Exception ex) {
      // We do not need to keep this operation because we throw an InternalException
      operationService.deleteOperations(operation.getId());
      Files.deleteIfExists(sipPath);
      throw new InternalException(
          "Failed to ingest archive",
          String.format("Failed to create ingest path: '%s'", sipPath),
          ex);
    }
    // Create and submit task
    processingService.submit(new IngestionTask(operation, this));
    return operation.getId();
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
        "Failed to download DIP",
        String.format("Access contracts don't match: '%s - '%s'", acIdentifier, contract));
  }

  public InputStream getManifestStream(Long tenant, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      return storageDao.getManifestStream(tenant, offers, id);
    }
  }

  /*
   * As the NF-461 says, we must only generate an ATR when the ingest operation has succeed :
   * "L’attestation est produite lorsque l’objet numérique et ses métadonnées sont dûment enregistrés sur
   * l’ensemble des sites conservation, spécifiés dans la politique d’archivage électronique ou la convention
   * d’archivage, connus du PA et conformes aux dispositions d’architecture."
   *
   * So, when an ATR of a failed ingest operation is requested, we throw a Not Found Exception.
   */
  public InputStream getXmlAtrStream(Long tenant, Long id) throws IOException, XMLStreamException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream is = storageDao.getAtrStream(tenant, offers, id)) {
      ArchiveTransferReply atr = JsonService.toArchiveTransferReply(is);
      return XmlATR.createOkInputStream(atr);
    }
  }

  public InputStream getJsonAtrStream(Long tenant, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      return storageDao.getAtrStream(tenant, offers, id);
    }
  }

  public void check(AbstractManifestParser parser, OperationDb operation) {
    Path sipPath = getSipPath(operation);
    Path unzipDir = getUnzipPath(operation);

    try {
      if (Files.exists(unzipDir)) {
        throw new FileAlreadyExistsException(unzipDir.toString());
      }
      Files.createDirectories(unzipDir);

      // Check zip exists and is readable
      if (Files.notExists(sipPath) || !Files.isReadable(sipPath) || Files.isDirectory(sipPath)) {
        throw new InternalException(String.format("Path %s not found", sipPath));
      }

      // Check if zip file is larger than 10 Go
      if (Files.size(sipPath) > 10_000_000_000L) {
        throw new BadRequestException(
            String.format("Archive is too big '%d'", Files.size(sipPath)));
      }

      // Check AV
      if (antiVirusScanner.getName() != AntiVirus.None) {
        ScanResult scanResult = antiVirusScanner.scan(sipPath);
        if (scanResult.status() != ScanStatus.OK) {
          throw new BadRequestException(
              String.format("Archive is not virus safe: %s", scanResult.detail()));
        }
      }

      // Unzip (protect against Zip bomb & Zip slip)
      ZipUtils.unzip(sipPath, unzipDir);
      Files.delete(sipPath);

      Path manifestPath = unzipDir.resolve(Env.MANIFEST_XML);
      if (Files.notExists(manifestPath) || Files.isDirectory(manifestPath)) {
        throw new FileNotFoundException("Archive does not contain a manifest");
      }

      // Log - change to debug
      if (log.isDebugEnabled()) {
        log.debug(
            "operationid: {}  - manifest {} ", operation.getId(), Files.readString(manifestPath));
      }

      // Validate manifest against Seda v2 xsd
      Sedav2Validator sedav2Validator = Sedav2Utils.getSedav2Validator(manifestPath);
      sedav2Validator.validate(manifestPath);
      String sedaVersion = sedav2Validator.getSedaVersion();

      // Parse manifest, check coherency and convert to json
      parser.parse(sedaVersion, manifestPath, unzipDir);

      // Log - change to debug
      if (log.isDebugEnabled()) {
        log.debug(
            "operationid: {} - archivetransfer: {}",
            operation.getId(),
            parser.getArchiveTransfer());
        parser
            .getArchiveUnits()
            .forEach(u -> log.debug("operationid: {} - archiveunit: {}", operation.getId(), u));
      }

    } catch (IOException | SAXException | XMLStreamException ex) {
      throw new InternalException(ex);
    }
  }

  public void commit(
      OperationDb operation,
      TenantDb tenantDb,
      List<ArchiveUnit> archiveUnits,
      ArchiveTransfer archiveTransfer,
      ManagementMetadata managementMetadata,
      List<DataObjectGroup> dataObjectGroups,
      IngestContractDb ingestContractDb) {

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> storageOffers = tenantDb.getStorageOffers();

    Path unzipDir = getUnzipPath(operation);
    List<StorageObject> storageObjects = new ArrayList<>();
    List<StorageObject> atrObjects = new ArrayList<>();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // Add Manifest
      if (BooleanUtils.isTrue(ingestContractDb.getStoreManifest())) {
        Path manifestPath = unzipDir.resolve(Env.MANIFEST_XML);
        storageObjects.add(new PathStorageObject(manifestPath, operationId, StorageObjectType.mft));
      }

      // Pack and encrypt all binary files
      Path binPath = unzipDir.resolve(operationId + "." + StorageObjectType.bin);
      try (Packer packer = storageDao.createPacker(binPath)) {
        for (ArchiveUnit unit : archiveUnits) {
          for (Qualifiers qualifiers : unit.getQualifiers()) {
            if (qualifiers.isBinaryQualifier()) {
              for (ObjectVersion bdo : qualifiers.getVersions()) {
                bdo.setOperationId(operationId);
                bdo.setPos(packer.write(bdo.getBinaryPath()));
              }
            } else {
              for (ObjectVersion objectVersion : qualifiers.getVersions()) {
                objectVersion.setOperationId(operationId);
              }
            }
          }
        }
      }

      // We ignore encryption because the binPath is already encrypted by the packer
      if (Files.exists(binPath)) {
        storageObjects.add(
            new PathStorageObject(binPath, operationId, StorageObjectType.bin, false, true));
      }

      // Add Archive Units
      byte[] units = JsonService.collToBytes(archiveUnits, JsonConfig.DEFAULT);
      storageObjects.add(new ByteStorageObject(units, operationId, StorageObjectType.uni));

      // Add Operation to storage (This eventually allows to restore operation from offer)
      byte[] ops = JsonService.toBytes(operation, JsonConfig.DEFAULT);
      storageObjects.add(new ByteStorageObject(ops, operationId, StorageObjectType.ope, true));

      // Write to offers then create actions
      storageDao
          .putStorageObjects(tenant, storageOffers, storageObjects)
          .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));

      // Write ATR when all others objects were successfully written to offers
      byte[] atr =
          JsonATR.toBytes(archiveTransfer, managementMetadata, dataObjectGroups, archiveUnits);
      atrObjects.add(new ByteStorageObject(atr, operationId, StorageObjectType.atr));

      // Write to offers then create actions
      storageDao
          .putStorageObjects(tenant, storageOffers, atrObjects)
          .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));

    } catch (Exception ex) {
      // Rollback (best effort!)
      storageService.deleteObjectsQuietly(storageOffers, tenant, storageObjects);
      storageService.deleteObjectsQuietly(storageOffers, tenant, atrObjects);
      throw new InternalException(ex);
    }
  }

  public void index(OperationDb operation, List<ArchiveUnit> archiveUnits) {
    try {
      // The operation is written to database after the indexation
      operation.setStatus(OperationStatus.OK);
      operation.setOutcome(operation.getStatus().toString());
      operation.setTypeInfo(operation.getType().getInfo());
      operation.setMessage("Operation completed with success");

      searchService.bulkIndex(archiveUnits);
      logbookService.index(operation);
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private Path getSipPath(OperationDb operation) {
    return Workspace.getPath(operation, "sip.zip");
  }

  private Path getUnzipPath(OperationDb operation) {
    return Workspace.getPath(operation, "sip");
  }
}
