/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.service;

import static fr.xelians.esafe.common.utils.ExceptionsUtils.format;

import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.logbook.domain.search.LogbookIndex;
import fr.xelians.esafe.logbook.domain.search.LogbookParser;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.dto.vitam.VitamExternalEventDto;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.search.domain.dsl.bucket.Facet;
import fr.xelians.esafe.search.service.SearchEngineService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogbookService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final int LOGBOOK_BULK_SIZE = 5000;

  private final LogbookMapper logbookOperationMapper;
  private final SearchEngineService searchEngineService;
  private final OperationService operationService;

  public LogbookOperationDto toLogbookOperationDto(LogbookOperation logbookOperation) {
    return logbookOperationMapper.toLogbookOperationDto(logbookOperation);
  }

  public VitamLogbookOperationDto toVitamLogbookOperationDto(LogbookOperation logbookOperation) {
    return logbookOperationMapper.toVitamLogbookOperationDto(logbookOperation);
  }

  public void index(OperationDb operation) throws IOException {
    index(operation.toOperationSe());
  }

  public void index(LogbookOperation operation) throws IOException {
    searchEngineService.index(LogbookIndex.ALIAS, operation);
  }

  public void bulkIndex(List<LogbookOperation> operations) throws IOException {
    for (List<LogbookOperation> list : ListUtils.partition(operations, LOGBOOK_BULK_SIZE)) {
      searchEngineService.bulkIndex(LogbookIndex.ALIAS, list);
    }
  }

  public LogbookOperation getOperationSe(Long tenant, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, "Id  must be not null");

    GetRequest request =
        new GetRequest.Builder().index(LogbookIndex.ALIAS).id(String.valueOf(id)).build();
    LogbookOperation logbookOperation =
        searchEngineService.getById(request, LogbookOperation.class);

    if (logbookOperation == null || !tenant.equals(logbookOperation.getTenant())) {
      throw new NotFoundException(
          "Logbook operation not found",
          String.format(
              "Failed to find logbook operation with id: '%s' - tenant: '%s'",
              request.id(), tenant));
    }
    return logbookOperation;
  }

  public VitamLogbookOperationDto getVitamLogbookOperationDto(Long tenant, Long id)
      throws IOException {
    return toVitamLogbookOperationDto(getOperationSe(tenant, id));
  }

  public LogbookOperationDto getLogbookOperationDto(Long tenant, Long id) throws IOException {
    return toLogbookOperationDto(getOperationSe(tenant, id));
  }

  public SearchResult<LogbookOperationDto> searchLogbookOperationDto(Long tenant, Long id)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, "Id  must be not null");
    return searchLogbookOperationDtos(tenant, queryById(id));
  }

  public SearchResult<VitamLogbookOperationDto> searchVitamLogbookOperationDto(Long tenant, Long id)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, "Id  must be not null");
    return searchVitamLogbookOperationDtos(tenant, queryById(id));
  }

  private SearchQuery queryById(Long id) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("#id", id.toString()));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    return SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
  }

  public SearchResult<VitamLogbookOperationDto> searchVitamLogbookOperationDtos(
      Long tenant, SearchQuery searchQuery) throws IOException {
    return search(tenant, searchQuery, this::toVitamLogbookOperationDto);
  }

  public SearchResult<LogbookOperationDto> searchLogbookOperationDtos(
      Long tenant, SearchQuery searchQuery) throws IOException {
    return search(tenant, searchQuery, this::toLogbookOperationDto);
  }

  private <T> SearchResult<T> search(
      Long tenant, SearchQuery query, Function<LogbookOperation, T> mapper) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(query, "Query must be not null");

    LogbookParser parser = LogbookParser.create(tenant);

    try {
      SearchRequest request = parser.createSearchRequest(query);
      log.info("Search JSON query: {}", JsonUtils.toJson(request));
      SearchResponse<LogbookOperation> response =
          searchEngineService.search(request, LogbookOperation.class);

      HitsMetadata<LogbookOperation> hitsMeta = response.hits();
      Hits hit = Hits.create(request, hitsMeta);

      List<Facet> facets = SearchEngineService.getFacets(response.aggregations());
      List<T> nodes =
          hitsMeta.hits().stream()
              .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
              .map(mapper)
              .toList();
      return new SearchResult<>(HttpStatus.OK.value(), hit, nodes, facets, query);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", query), ex);
    }
  }

  public Long createExternalLogbookOperation(
      Long tenant, String user, String app, VitamExternalEventDto event) {

    // Create operation
    OperationDb operation = OperationFactory.createExternalOp(tenant);
    setLastEvent(operation, event);
    setFirstEvent(operation, event, user, app);

    operation.setStatus(OperationStatus.RUN);
    operation = operationService.save(operation);

    // Try to index operation
    indexOperation(operation);

    return operation.getId();
  }

  private static void setLastEvent(OperationDb operation, VitamExternalEventDto parentEvent) {
    var events = parentEvent.getEvents();
    if (!events.isEmpty()) {
      VitamExternalEventDto event = events.getLast();

      operation.setCreated(event.getEventDateTime());
      operation.setModified(event.getEventDateTime());
      operation.setTypeInfo(event.getEventType());
      operation.setOutcome(event.getOutcome());
      operation.setMessage(event.getOutcomeDetailMessage());
      operation.setObjectIdentifier(event.getObjectIdentifier());
      operation.setObjectInfo(event.getObjectIdentifierRequest());
      operation.setObjectData(event.getEventDetailData());
      operation.setUserIdentifier(event.getAgentIdentifier());

      if (StringUtils.isNotBlank(event.getAgentIdentifierApplicationSession())) {
        operation.setApplicationId(event.getAgentIdentifierApplicationSession());
      } else {
        operation.setApplicationId(event.getAgentIdentifierApplication());
      }
    }
  }

  private static void setFirstEvent(
      OperationDb operation, VitamExternalEventDto parentEvent, String user, String app) {
    if (parentEvent.getEventDateTime() != null) {
      operation.setCreated(parentEvent.getEventDateTime());
    }
    if (operation.getCreated() == null) {
      operation.setCreated(LocalDateTime.now());
    }
    if (operation.getModified() == null) {
      operation.setModified(operation.getCreated());
    }
    if (StringUtils.isBlank(operation.getTypeInfo())
        || OperationType.EXTERNAL.toString().equals(operation.getTypeInfo())) {
      operation.setTypeInfo(parentEvent.getEventType());
    }
    if (StringUtils.isBlank(operation.getOutcome())) {
      operation.setOutcome(parentEvent.getOutcome());
    }
    if (StringUtils.isBlank(operation.getMessage())) {
      operation.setMessage(parentEvent.getOutcomeDetailMessage());
    }
    if (StringUtils.isBlank(operation.getObjectIdentifier())) {
      operation.setObjectIdentifier(parentEvent.getObjectIdentifier());
    }
    if (StringUtils.isBlank(operation.getObjectInfo())) {
      operation.setObjectInfo(parentEvent.getObjectIdentifierRequest());
    }
    if (StringUtils.isBlank(operation.getObjectData())) {
      operation.setObjectData(parentEvent.getEventDetailData());
    }

    if (StringUtils.isBlank(operation.getUserIdentifier())) {
      if (StringUtils.isNotBlank(parentEvent.getAgentIdentifier())) {
        operation.setUserIdentifier(parentEvent.getAgentIdentifier());
      } else {
        operation.setUserIdentifier(user);
      }
    }

    if (StringUtils.isBlank(operation.getApplicationId())) {
      if (StringUtils.isNotBlank(parentEvent.getAgentIdentifierApplicationSession())) {
        operation.setApplicationId(parentEvent.getAgentIdentifierApplicationSession());
      } else if (StringUtils.isNotBlank(parentEvent.getAgentIdentifierApplication())) {
        operation.setApplicationId(parentEvent.getAgentIdentifierApplication());
      } else {
        operation.setApplicationId(app);
      }
    }
  }

  public void indexOperation(OperationDb operation) {
    try {
      index(operation);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Operation completed with success");
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Index operation failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage(String.format("Failed to index - retrying - Code: %s", e.getCode()));
      operationService.save(operation);
    }
  }
}
