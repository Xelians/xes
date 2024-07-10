/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.accession.domain.model.*;
import fr.xelians.esafe.accession.domain.model.RegisterSummary;
import fr.xelians.esafe.accession.domain.search.RegisterDetailsIndex;
import fr.xelians.esafe.accession.domain.search.RegisterDetailsParser;
import fr.xelians.esafe.accession.domain.search.RegisterSummaryIndex;
import fr.xelians.esafe.accession.domain.search.RegisterSummaryParser;
import fr.xelians.esafe.accession.dto.RegisterDetailsDto;
import fr.xelians.esafe.accession.dto.RegisterSummaryDto;
import fr.xelians.esafe.admin.domain.report.EliminationReportDto;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.search.domain.dsl.bucket.Facet;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessionRegisterService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final int ACCESSION_BULK_SIZE = 5000;

  private final RegisterMapper registerMapper;
  private final TenantService tenantService;
  private final OperationService operationService;
  private final StorageService storageService;
  private final SearchEngineService searchEngineService;

  public RegisterSummaryDto toRegisterSummaryDto(RegisterSummary registerSummary) {
    return registerMapper.toRegisterSummaryDto(registerSummary);
  }

  public RegisterDetailsDto toRegisterDetailsDto(RegisterDetails registerDetails) {
    return registerMapper.toRegisterDetailsDto(registerDetails);
  }

  public SearchResult<RegisterSummaryDto> searchSummary(Long tenant, SearchQuery query)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "Query must be not null");

    RegisterSummaryParser parser = RegisterSummaryParser.create(tenant);

    try {
      SearchRequest request = parser.createSearchRequest(query);
      log.info("Search JSON query: {}", JsonUtils.toJson(request));
      SearchResponse<RegisterSummary> response =
          searchEngineService.search(request, RegisterSummary.class);

      HitsMetadata<RegisterSummary> hitsMeta = response.hits();
      Hits hits = Hits.create(request, hitsMeta);

      List<Facet> facets = SearchEngineService.getFacets(response.aggregations());
      List<RegisterSummaryDto> dtos =
          SearchEngineService.getResults(hitsMeta, this::toRegisterSummaryDto);
      return new SearchResult<>(HttpStatus.OK.value(), hits, dtos, facets, query);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", query), ex);
    }
  }

  public SearchResult<RegisterDetailsDto> searchDetails(Long tenant, SearchQuery query)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "Query must be not null");

    RegisterDetailsParser parser = RegisterDetailsParser.create(tenant);

    try {
      SearchRequest request = parser.createSearchRequest(query);
      SearchResponse<RegisterDetails> response =
          searchEngineService.search(request, RegisterDetails.class);

      HitsMetadata<RegisterDetails> hitsMeta = response.hits();
      Hits hit = Hits.create(request, hitsMeta);

      List<Facet> facets = SearchEngineService.getFacets(response.aggregations());
      List<RegisterDetailsDto> dtos =
          SearchEngineService.getResults(hitsMeta, this::toRegisterDetailsDto);
      return new SearchResult<>(HttpStatus.OK.value(), hit, dtos, facets, query);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", query), ex);
    }
  }

  public void registerOperations(List<OperationDb> ops) throws IOException {
    Map<Long, RegisterDetails> detailsMap = new HashMap<>();
    Map<String, RegisterSummary> summaryMap = new HashMap<>();

    TenantDb tenantDb;
    Long tenant = null;
    List<String> offers = null;
    StorageDao storageDao = null;

    try {
      for (OperationDb operation : ops) {
        if (!Objects.equals(tenant, operation.getTenant())) {
          tenant = operation.getTenant();
          tenantDb = tenantService.getTenantDb(tenant);
          offers = tenantDb.getStorageOffers();
          if (storageDao != null) storageDao.close();
          storageDao = storageService.createStorageDao(tenantDb);
        }
        OperationType type = operation.getType();

        if (OperationType.isIngest(type)) {
          try (InputStream is = storageDao.getAtrStream(tenant, offers, operation.getId())) {
            ArchiveTransferReply atr = JsonService.toArchiveTransferReply(is);
            registerIngestDetails(operation, detailsMap, atr);
            registerIngestSummary(operation, summaryMap, atr);
          }
        } else if (type == OperationType.ELIMINATE_ARCHIVE) {
          try (InputStream is = storageDao.getReportStream(tenant, offers, operation.getId())) {
            // TODO Stream the report to prevent OOM
            EliminationReportDto report = JsonService.to(is, EliminationReportDto.class);
            registerEliminationDetails(operation, detailsMap, report);
            registerEliminationSummary(operation, summaryMap, report);
          }
        }
      }
    } finally {
      if (storageDao != null) storageDao.close();
    }

    // ElasticSearch durability is ensured by the translog. Hence, we normally don't need to flush
    // (i.e. to do a Lucene commit) but we provide "ceinture et bretelles". In case of a failure,
    // an exception will be thrown and the operation will not be saved into the database

    // Index accession register details
    indexDetails(new ArrayList<>(detailsMap.values()));
    searchEngineService.flush(RegisterDetailsIndex.ALIAS);

    // Index accession register summary
    var summaries = new ArrayList<>(summaryMap.values());
    indexSummary(summaries);
    searchEngineService.flush(RegisterSummaryIndex.ALIAS);

    // Save all operations to database
    for (OperationDb operation : ops) {
      operationService.updateRegistered(operation);
    }

    // Remove temporary OperationIds from summary
    summaries.forEach(s -> s.setOperationIds(Collections.emptyList()));
    indexSummary(summaries);

    // Ensure that all modifications are visible
    searchEngineService.refresh(RegisterDetailsIndex.ALIAS);
    searchEngineService.refresh(RegisterSummaryIndex.ALIAS);
  }

  private void registerIngestDetails(
      OperationDb operation, Map<Long, RegisterDetails> detailsMap, ArchiveTransferReply atr)
      throws IOException {

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();

    // Get register details
    if (detailsMap.containsKey(operationId)) {
      throw new InternalException(
          "Failed to create register details",
          String.format("Register details is already created for operation '%s'", operationId));
    }

    // Ensure idempotency by checking if the operation was already done
    Optional<RegisterDetails> optDetails = getRegisterDetails(tenant, operationId);
    if (optDetails.isPresent()) {
      detailsMap.put(operationId, optDetails.get());
      return;
    }

    RegisterDetails details = new RegisterDetails();
    detailsMap.put(operationId, details);

    details.setId(operationId);
    details.setOpi(operationId);
    details.setOpc(operationId);
    details.addOperationId(operationId);
    details.setTenant(tenant);
    details.incVersion();

    details.setAcquisitionInformation(atr.getAcquisitionInformation());
    details.setArchivalAgreement(atr.getArchivalAgreement());
    details.setArchivalProfile(atr.getArchivalProfile());
    details.setLegalStatus(atr.getLegalStatus());
    details.setStartDate(atr.getGrantDate());
    details.setEndDate(atr.getDate());
    details.setLastUpdate(atr.getDate());
    details.setStatus(RegisterStatus.STORED_AND_COMPLETED);
    details.setOperationType("INGEST");
    details.setOriginatingAgency(atr.getArchivalAgencyIdentifier());
    details.setSubmissionAgency(atr.getTransferringAgencyIdentifier());
    if (StringUtils.isNotBlank(atr.getComment())) {
      details.addComment(atr.getComment());
    }

    ValueEvent event = createValueEvent(atr);
    details.addEvent(event);

    details.setTotalObjects(new ValueDetail(event.getTotalObjects()));
    details.setTotalUnits(new ValueDetail(event.getTotalUnits()));
    details.setTotalObjectsGroups(new ValueDetail(event.getObjectsGroups()));
    details.setObjectSize(new ValueDetail(event.getObjectSize()));
  }

  private void registerEliminationDetails(
      OperationDb operation, Map<Long, RegisterDetails> detailsMap, EliminationReportDto report)
      throws IOException {

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    Map<Long, ValueEvent> eventMap = new HashMap<>();

    // List deleted object
    for (EliminationReportDto.ArchiveUnitDto au : report.archiveUnits()) {
      Long opId = au.operationId();

      // Get register details
      RegisterDetails details = detailsMap.get(opId);
      if (details == null) {
        details = getRegisterDetails(tenant, opId).orElseThrow(() -> detailsNotFound(opId));
        detailsMap.put(opId, details);
      }

      // Ensure idempotency by checking if the operation was already done
      if (details.getEvents().stream()
          .map(ValueEvent::getOperationId)
          .anyMatch(operationId::equals)) {
        continue;
      }

      long totalObjects = au.getTotalObjects();
      long sizeOfBinaryObjects = au.getSizeOfBinaryObjects();

      // Create event
      ValueEvent event = eventMap.get(opId);
      if (event == null) {
        event = new ValueEvent();
        event.setOperationType("ELIMINATION");
        event.setCreationDate(operation.getCreated());
        event.setTotalObjects(-totalObjects);
        event.setTotalUnits(-1L);
        event.setObjectsGroups(totalObjects > 0 ? -1L : 0);
        event.setObjectSize(-sizeOfBinaryObjects);
        eventMap.put(opId, event);
      } else {
        event.setTotalObjects(event.getTotalObjects() - totalObjects);
        event.setTotalUnits(event.getTotalUnits() - 1L);
        event.setObjectsGroups(event.getObjectsGroups() + (totalObjects > 0 ? -1L : 0));
        event.setObjectSize(event.getObjectSize() - sizeOfBinaryObjects);
      }
    }

    // Update details with event
    eventMap.forEach(
        (opId, event) -> {
          RegisterDetails details = detailsMap.get(opId);
          details.incVersion();
          details.setOpc(operationId);
          details.addOperationId(operationId);
          details.setStatus(RegisterStatus.STORED_AND_UPDATED);
          details.getTotalObjects().addDeleted(event.getTotalObjects());
          details.getTotalUnits().addDeleted(event.getTotalUnits());
          details.getTotalObjectsGroups().addDeleted(event.getObjectsGroups());
          details.getObjectSize().addDeleted(event.getObjectSize());
          details.addEvent(event);
        });
  }

  private static @NotNull InternalException detailsNotFound(Long opId) {
    return new InternalException(
        "Failed to create register details",
        String.format("Details does not exist for operation '%s'", opId));
  }

  private Optional<RegisterDetails> getRegisterDetails(Long tenant, Long id) throws IOException {
    RegisterDetailsParser parser = RegisterDetailsParser.create(tenant);
    SearchRequest request = parser.createSearchRequest(queryById(id));
    SearchResponse<RegisterDetails> response =
        searchEngineService.search(request, RegisterDetails.class);
    HitsMetadata<RegisterDetails> hits = response.hits();
    TotalHits total = hits.total();
    return (total == null || total.value() == 0)
        ? Optional.empty()
        : Optional.ofNullable(hits.hits().getFirst().source());
  }

  private static @NotNull ValueEvent createValueEvent(ArchiveTransferReply atr) {
    ValueEvent event = new ValueEvent();
    event.setOperationId(atr.getOperationId());
    event.setOperationType("INGEST");
    event.setCreationDate(atr.getGrantDate());
    event.setTotalUnits((long) atr.getNumOfUnits());
    event.setObjectsGroups((long) atr.getNumOfObjectGroups());
    event.setObjectSize(atr.getSizeOfBinaryObjects());
    event.setTotalObjects(
        (long) atr.getNumOfBinaryObjects() + (long) atr.getNumOfPhysicalObjects());
    return event;
  }

  private void registerIngestSummary(
      OperationDb operation, Map<String, RegisterSummary> summaryMap, ArchiveTransferReply atr)
      throws IOException {

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    String agency = atr.getArchivalAgencyIdentifier();

    RegisterSummary summary = summaryMap.get(agency);
    if (summary == null) {
      summary =
          getRegisterSummary(tenant, agency)
              .orElseGet(() -> createRegisterSummary(operation, agency));
      summaryMap.put(agency, summary);
    }

    // Ensure idempotency by checking if the operation was already done
    if (!summary.getOperationIds().contains(operationId)) {
      summary.addOperationId(operationId);
      summary.incVersion();

      long totalObjects = (long) atr.getNumOfBinaryObjects() + (long) atr.getNumOfPhysicalObjects();
      summary.getTotalObjects().addIngested(totalObjects);
      summary.getTotalUnits().addIngested(atr.getNumOfUnits());
      summary.getTotalObjectsGroups().addIngested(atr.getNumOfObjectGroups());
      summary.getObjectSize().addIngested(atr.getSizeOfBinaryObjects());
    }
  }

  private void registerEliminationSummary(
      OperationDb operation, Map<String, RegisterSummary> summaryMap, EliminationReportDto report)
      throws IOException {

    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    Map<String, ValueEvent> eventMap = new HashMap<>();

    for (EliminationReportDto.ArchiveUnitDto au : report.archiveUnits()) {
      String agency = au.archivalAgencyIdentifier();

      RegisterSummary summary = summaryMap.get(agency);
      if (summary == null) {
        summary = getRegisterSummary(tenant, agency).orElseThrow(() -> summaryNotFound(agency));
        summaryMap.put(agency, summary);
      }

      // Ensure idempotency by checking if the operation was already done
      if (summary.getOperationIds().contains(operationId)) {
        continue;
      }

      long totalObjects = au.getTotalObjects();
      long sizeOfBinaryObjects = au.getSizeOfBinaryObjects();

      // Create event
      ValueEvent event = eventMap.get(agency);
      if (event == null) {
        event = new ValueEvent();
        event.setTotalObjects(-totalObjects);
        event.setTotalUnits(-1L);
        event.setObjectsGroups(totalObjects > 0 ? -1L : 0);
        event.setObjectSize(-sizeOfBinaryObjects);
        eventMap.put(agency, event);
      } else {
        event.setTotalObjects(event.getTotalObjects() - totalObjects);
        event.setTotalUnits(event.getTotalUnits() - 1L);
        event.setObjectsGroups(event.getObjectsGroups() + (totalObjects > 0 ? -1L : 0));
        event.setObjectSize(event.getObjectSize() - sizeOfBinaryObjects);
      }
    }

    // Update summary with event
    eventMap.forEach(
        (agency, event) -> {
          RegisterSummary summary = summaryMap.get(agency);
          summary.incVersion();
          summary.addOperationId(operationId);
          summary.getTotalObjects().addDeleted(event.getTotalObjects());
          summary.getTotalUnits().addDeleted(event.getTotalUnits());
          summary.getTotalObjectsGroups().addDeleted(event.getObjectsGroups());
          summary.getObjectSize().addDeleted(event.getObjectSize());
        });
  }

  private static @NotNull InternalException summaryNotFound(String agency) {
    return new InternalException(
        "Failed to create register summary",
        String.format("Summary does not exist for agency '%s'", agency));
  }

  private static @NotNull RegisterSummary createRegisterSummary(
      OperationDb operation, String oriAgency) {
    RegisterSummary summary = new RegisterSummary();
    summary.setId(operation.getId());
    summary.setOriginatingAgency(oriAgency);
    summary.setTenant(operation.getTenant());
    return summary;
  }

  private Optional<RegisterSummary> getRegisterSummary(Long tenant, String agency)
      throws IOException {

    RegisterSummaryParser parser = RegisterSummaryParser.create(tenant);
    SearchRequest request = parser.createSearchRequest(queryByAgency(agency));
    SearchResponse<RegisterSummary> response =
        searchEngineService.search(request, RegisterSummary.class);

    HitsMetadata<RegisterSummary> hits = response.hits();
    TotalHits total = hits.total();
    return (total == null || total.value() == 0)
        ? Optional.empty()
        : Optional.ofNullable(hits.hits().getFirst().source());
  }

  public void indexDetails(List<RegisterDetails> details) {
    try {
      for (List<RegisterDetails> list : ListUtils.partition(details, ACCESSION_BULK_SIZE)) {
        searchEngineService.bulkIndex(RegisterDetailsIndex.ALIAS, list);
      }
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  public void indexSummary(List<RegisterSummary> summaries) {
    try {
      for (List<RegisterSummary> list : ListUtils.partition(summaries, ACCESSION_BULK_SIZE)) {
        searchEngineService.bulkIndex(RegisterSummaryIndex.ALIAS, list);
      }
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private static SearchQuery queryById(Long id) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("#id", id.toString()));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    return SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
  }

  private static SearchQuery queryByAgency(String agency) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("OriginatingAgency", agency));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    return SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
  }
}
