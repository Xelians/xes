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
import fr.xelians.esafe.archive.domain.elimination.Eliminator;
import fr.xelians.esafe.archive.domain.elimination.RuleEliminator;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationParser;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationQuery;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationRequest;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationResult;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.task.EliminationTask;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class EliminationService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";
  public static final String FAILED_TO_ELIMINATE = "Failed to eliminate archives";

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

  public Eliminator check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();
    if (tenantDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          FAILED_TO_ELIMINATE, String.format("Tenant '%s' is not active", tenant));
    }

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {

      // TODO  Optimisation: only retrieve relevant fields of archive unit
      String query = operation.getProperty02();
      EliminationResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);
      List<ArchiveUnit> selectedUnits = result.results();
      LocalDate eliminationDate = result.eliminationDate();
      if (LocalDate.now().isBefore(eliminationDate)) {
        throw new BadRequestException(
            FAILED_TO_ELIMINATE,
            String.format(
                "Cannot eliminate archive units with a future elimination date '%s'",
                eliminationDate));
      }

      Path tmpAusPath = Workspace.createTempFile(operation);
      Eliminator eliminator =
          new RuleEliminator(
              searchService,
              storageService,
              tenantDb,
              accessContract,
              tmpAusPath,
              selectedUnits,
              eliminationDate);
      eliminator.check(storageDao);

      return eliminator;
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
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

      SearchRequest searchRequest = eliminationRequest.searchRequest();
      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(searchRequest, ArchiveUnit.class);

      // TODO check if detail overflows
      List<ArchiveUnit> nodes = response.hits().hits().stream().map(Hit::source).toList();
      return new EliminationResult<>(nodes, eliminationQuery.eliminationDate());

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed",
          String.format("Failed to parse query '%s'", eliminationQuery),
          ex);
    }
  }

  public void storeOperation(OperationDb operation) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb);
        InputStream ausStream = storageDao.getAusStream(tenant, offers, operationId)) {

      Eliminator eliminator = new RuleEliminator(searchService, storageService, tenantDb);
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
