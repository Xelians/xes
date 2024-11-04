/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.admin.domain.report.ArchiveReport;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.service.AdminService;
import fr.xelians.esafe.archive.domain.atr.Reply;
import fr.xelians.esafe.archive.domain.elimination.Eliminator;
import fr.xelians.esafe.archive.domain.elimination.TransferEliminator;
import fr.xelians.esafe.archive.domain.ingest.sedav2.XmlATR;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.task.TransferReplyTask;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.*;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.xml.stream.XMLStreamException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.xom.ParsingException;
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
public class TransferReplyService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String TRANSFER_FAILED = "Failed to transfert archives";
  public static final String STREAM_MUST_BE_NOT_NULL = "Stream must be not null";

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

  public Long transferReply(
      Long tenant, String accessContract, InputStream inputStream, String user, String app)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(inputStream, STREAM_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    // Check if Access Contract exists and is active
    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          TRANSFER_FAILED, String.format("Access Contract '%s' is inactive", accessContractDb));
    }

    // Create operation
    OperationDb operation =
        OperationFactory.transferReplyArchiveOp(tenant, accessContract, user, app);
    operation = operationService.save(operation);

    // Create temporary dir
    Path ws = Workspace.getPath(operation);
    if (Files.exists(ws)) {
      throw new FileAlreadyExistsException(ws.toString());
    }
    Files.createDirectories(ws);

    // Create path with operation id
    Path atrPath = Workspace.getPath(operation, "atr.xml");

    try {
      Files.copy(inputStream, atrPath);
    } catch (Exception ex) {
      // We do not need to keep this operation because we throw an InternalException
      operationService.deleteOperations(operation.getId());
      Files.deleteIfExists(atrPath);
      throw new InternalException(
          TRANSFER_FAILED, String.format("Failed to create atr path: '%s'", atrPath), ex);
    }

    // Create and submit task
    processingService.submit(new TransferReplyTask(operation, this));
    return operation.getId();
  }

  public Eliminator check(OperationDb operation, TenantDb tenantDb) {
    Long tenant = tenantDb.getId();

    if (tenantDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          "Failed to transfert archives", String.format("Tenant '%s' is not active", tenant));
    }

    List<String> offers = tenantDb.getStorageOffers();

    Path atrPath = Workspace.getPath(operation, "atr.xml");
    if (!Files.exists(atrPath)) {
      throw new InternalException(TRANSFER_FAILED, "Transfer reply file does not exist");
    }

    Reply reply;

    try {
      reply = XmlATR.createReply(atrPath);

      if (!"OK".equalsIgnoreCase(reply.replyCode())) {
        throw new BadRequestException(
            TRANSFER_FAILED,
            String.format("Archive Transfer Reply completes with code '%s'", reply.replyCode()));
      }
    } catch (IOException | ParsingException | XMLStreamException ex) {
      throw new BadRequestException(TRANSFER_FAILED, "Failed to parse ATR");
    }

    String transferId = reply.messageIdentifier();
    if (!StringUtils.isNumeric(transferId)) {
      throw new BadRequestException(
          TRANSFER_FAILED,
          String.format(
              "Transfer reply does not contain a valid message identifier: '%s'", transferId));
    }

    // Write transfer report to offer
    ArchiveReport report;
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream is = storageDao.getReportStream(tenant, offers, Long.valueOf(transferId))) {

      report = JsonService.to(is, ArchiveReport.class);
      if (report.status() != ReportStatus.OK) {
        throw new BadRequestException(
            TRANSFER_FAILED,
            String.format(
                "Transfer report '%s' failed with status '%s'", transferId, report.status()));
      }
    } catch (IOException ex) {
      throw new BadRequestException(TRANSFER_FAILED, ex.getMessage());
    }

    // Get the accessContact
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getContractIdentifier());

    // Get the transferred archives from the report
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      List<ArchiveReport.ArchiveUnit> selectedUnits = report.archiveUnits();

      Path tmpAusPath = Workspace.createTempFile(operation);
      List<ArchiveUnit> archiveUnits = getArchiveUnits(tenantDb, accessContract, selectedUnits);
      Eliminator eliminator =
          new TransferEliminator(
              searchService, storageService, tenantDb, accessContract, tmpAusPath, archiveUnits);
      eliminator.check(storageDao);

      return eliminator;
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private List<ArchiveUnit> getArchiveUnits(
      TenantDb tenantDb,
      AccessContractDb accessContract,
      List<ArchiveReport.ArchiveUnit> selectedUnits)
      throws IOException {

    List<Long> ids = selectedUnits.stream().map(su -> Long.parseLong(su.systemId())).toList();
    List<ArchiveUnit> archiveUnits = new ArrayList<>();
    List<JsonNode> units = searchService.getUnitsByIds(tenantDb.getId(), accessContract, ids);
    for (var unit : units) {
      archiveUnits.add(JsonService.toArchiveUnit(unit));
    }
    return archiveUnits;
  }

  public void commit(OperationDb operation, TenantDb tenantDb, Eliminator eliminator) {
    Long operationId = operation.getId();
    Long tenant = tenantDb.getId();
    List<String> offers = tenantDb.getStorageOffers();

    List<StorageObject> storageObjects = new ArrayList<>();

    // Write Archive Units (.aus) from temporary file
    storageObjects.add(
        new PathStorageObject(eliminator.getPath(), operationId, StorageObjectType.aus, true));

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

  public void store(OperationDb operation, TenantDb tenantDb, Eliminator eliminator) {
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = Files.newInputStream(eliminator.getPath())) {
      eliminator.eliminate(operation, ausStream, storageDao);
    } catch (IOException ex) {
      NioUtils.deleteDirQuietly(eliminator.getPath());
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

  public void storeOperation(OperationDb operation) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = storageDao.getAusStream(tenant, offers, operationId)) {

      Eliminator eliminator = new TransferEliminator(searchService, storageService, tenantDb);
      eliminator.eliminate(operation, ausStream, storageDao);

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
}
