/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.MgetRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.mget.MultiGetOperation;
import co.elastic.clients.elasticsearch.core.search.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.converter.Converter;
import fr.xelians.esafe.archive.domain.converter.ObjectConverter;
import fr.xelians.esafe.archive.domain.converter.UnitConverter;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitIndex;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import fr.xelians.esafe.archive.domain.search.search.SearchParser;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.domain.search.search.StreamRequest;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.inherited.*;
import fr.xelians.esafe.archive.domain.unit.rules.management.AbstractRules;
import fr.xelians.esafe.archive.domain.unit.rules.management.Management;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.common.utils.StreamContent;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.search.domain.dsl.bucket.Facet;
import fr.xelians.esafe.search.domain.dsl.parser.NamedField;
import fr.xelians.esafe.search.domain.field.*;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
public class SearchService {

  // TODO  Should be an external property (note: 100MB is the maximum bulk size)
  public static final int ARCHIVE_UNIT_BULK_SIZE = 5000;
  public static final JsonNode STORAGE_NODE = getDefaultStorage();

  public static final String TENANT_FIELD = "_tenant";
  public static final String KEYWORDS_FIELD = "_keywords";
  public static final String EXT = Field.EXT + ".*";
  public static final String UPS = "_ups.*";

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "Archive unit id must be not null";
  public static final String ARCHIVE_UNIT_ID_MUST_BE_POSITIVE =
      "Archive unit id '%s' must be positive";

  private final TenantService tenantService;
  private final OntologyService ontologyService;
  private final StorageService storageService;
  private final SearchEngineService searchEngineService;
  private final LifecycleConverterService lifecycleConverterService;

  // Note. Elasticsearch limits the maximum size of a HTTP request to 100mb by default so clients
  // must ensure that no request exceeds this size.
  public void bulkIndex(List<ArchiveUnit> archiveUnits) throws IOException {
    for (List<ArchiveUnit> list : ListUtils.partition(archiveUnits, ARCHIVE_UNIT_BULK_SIZE)) {
      searchEngineService.bulkIndex(ArchiveUnitIndex.ALIAS, list);
    }
  }

  public void bulkIndexRefresh(List<ArchiveUnit> archiveUnits) throws IOException {
    if (archiveUnits.size() <= ARCHIVE_UNIT_BULK_SIZE) {
      searchEngineService.bulkIndexRefresh(ArchiveUnitIndex.ALIAS, archiveUnits);
    } else {
      for (List<ArchiveUnit> list : ListUtils.partition(archiveUnits, ARCHIVE_UNIT_BULK_SIZE)) {
        searchEngineService.bulkIndex(ArchiveUnitIndex.ALIAS, list);
      }
      searchEngineService.refresh(ArchiveUnitIndex.ALIAS);
    }
  }

  public void refresh() throws IOException {
    searchEngineService.refresh(ArchiveUnitIndex.ALIAS);
  }

  public void bulkDelete(List<Long> ids) throws IOException {
    for (List<Long> list : ListUtils.partition(ids, ARCHIVE_UNIT_BULK_SIZE)) {
      searchEngineService.bulkDelete(ArchiveUnitIndex.ALIAS, list);
    }
  }

  public void bulkDeleteRefresh(List<Long> ids) throws IOException {
    if (ids.size() <= ARCHIVE_UNIT_BULK_SIZE) {
      searchEngineService.bulkDeleteRefresh(ArchiveUnitIndex.ALIAS, ids);
    } else {
      for (List<Long> list : ListUtils.partition(ids, ARCHIVE_UNIT_BULK_SIZE)) {
        searchEngineService.bulkDelete(ArchiveUnitIndex.ALIAS, list);
      }
      searchEngineService.refresh(ArchiveUnitIndex.ALIAS);
    }
  }

  public JsonNode getRawUnit(Long tenant, AccessContractDb accessContract, Long id)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);
    Assert.isTrue(id >= 0, String.format(ARCHIVE_UNIT_ID_MUST_BE_POSITIVE, id));

    SearchQuery query = queryByUnitId(id);
    return getUnitByIdQuery(tenant, accessContract, query, id);
  }

  public JsonNode getUnit(Long tenant, AccessContractDb accessContract, Long id)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);
    Assert.isTrue(id >= 0, String.format(ARCHIVE_UNIT_ID_MUST_BE_POSITIVE, id));

    SearchQuery query = queryByUnitId(id);
    JsonNode unit = getUnitByIdQuery(tenant, accessContract, query, id);
    return UnitConverter.INSTANCE.convert(unit);
  }

  public SearchResult<JsonNode> searchUnit(Long tenant, AccessContractDb accessContract, Long id)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);
    Assert.isTrue(id >= 0, String.format(ARCHIVE_UNIT_ID_MUST_BE_POSITIVE, id));

    SearchQuery query = queryByUnitId(id);
    return searchUnits(tenant, accessContract, query, UnitConverter.INSTANCE);
  }

  public SearchResult<JsonNode> searchUnits(
      Long tenant, AccessContractDb accessContract, SearchQuery query, Converter converter)
      throws IOException {

    log.info("Search query: {}", query);
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(query, QUERY_MUST_BE_NOT_NULL);

    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser parser = SearchParser.create(tenant, accessContract, ontologyMapper);

    try {
      SearchRequest request = parser.createRequest(query);
      SearchResponse<JsonNode> response = searchEngineService.search(request, JsonNode.class);

      HitsMetadata<JsonNode> hitsMeta = response.hits();
      Hits hit = Hits.create(request, hitsMeta);

      List<JsonNode> nodes =
          hitsMeta.hits().stream()
              .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
              .filter(Objects::nonNull)
              .map(converter::convert)
              .toList();

      List<Facet> facets = SearchEngineService.getFacets(response.aggregations());
      return new SearchResult<>(HttpStatus.OK.value(), hit, nodes, facets, query);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", query), ex);
    }
  }

  public SearchResult<JsonNode> searchWithInheritedRules(
      Long tenant, AccessContractDb accessContract, SearchQuery query) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(query, QUERY_MUST_BE_NOT_NULL);

    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser parser = SearchParser.create(tenant, accessContract, ontologyMapper);

    try {
      SearchRequest request = parser.createWithInheritedRulesRequest(query);
      SearchResponse<JsonNode> response = searchEngineService.search(request, JsonNode.class);

      HitsMetadata<JsonNode> hitsMeta = response.hits();
      Hits hit = Hits.create(request, hitsMeta);

      // Search parents
      List<JsonNode> nodes = new ArrayList<>();
      Set<Long> parentSet = new HashSet<>();
      List<JsonNode> nodeList = new ArrayList<>();

      List<JsonNode> srcNodes =
          hitsMeta.hits().stream()
              .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
              .toList();
      for (JsonNode node : srcNodes) {
        nodeList.add(node);
        node.get("_us").forEach(v -> parentSet.add(v.asLong()));
        if (parentSet.size() > 1000) {
          searchParent(tenant, accessContract, ontologyMapper, parentSet, nodeList, nodes);
          parentSet.clear();
          nodeList.clear();
        }
      }
      if (!nodeList.isEmpty()) {
        searchParent(tenant, accessContract, ontologyMapper, parentSet, nodeList, nodes);
      }

      List<Facet> facets = SearchEngineService.getFacets(response.aggregations());
      return new SearchResult<>(HttpStatus.OK.value(), hit, nodes, facets, query);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", query), ex);
    }
  }

  private void searchParent(
      Long tenant,
      AccessContractDb accessContract,
      OntologyMapper ontologyMapper,
      Set<Long> parentSet,
      List<JsonNode> nodeList,
      List<JsonNode> nodes)
      throws IOException {

    SearchRequest searchRequest =
        createParentsRequest(tenant, accessContract, ontologyMapper, parentSet);
    SearchResponse<ArchiveUnit> response =
        searchEngineService.search(searchRequest, ArchiveUnit.class);

    Map<Long, ArchiveUnit> parentMap =
        response.hits().hits().stream()
            .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
            .collect(Collectors.toMap(ArchiveUnit::getId, Function.identity()));

    for (JsonNode node : nodeList) {
      JsonNode irNode = createInheritedRules(node, parentMap);
      nodes.add(UnitConverter.convertWithInheritedRules(node, irNode));
    }
  }

  private SearchRequest createParentsRequest(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, Set<Long> ids) {

    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ArrayNode idsNode = idNode.putArray("#id");
    ids.stream().map(Object::toString).forEach(idsNode::add);
    ObjectNode inNode = JsonNodeFactory.instance.objectNode();
    inNode.set("$in", idNode);

    ObjectNode typeNode = JsonNodeFactory.instance.objectNode();
    ObjectNode neqNode = JsonNodeFactory.instance.objectNode();
    neqNode.set("$neq", typeNode.put("#unitType", UnitType.FILING_UNIT.toString()));

    ObjectNode andNode = JsonNodeFactory.instance.objectNode();
    ArrayNode andNodes = andNode.putArray("$and");
    andNodes.add(inNode);
    andNodes.add(neqNode);

    // TODO Optimize by limiting the projection
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();

    SearchQuery searchQuery =
        SearchQuery.builder().queryNode(andNode).projectionNode(projectionNode).build();

    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);
    return searchParser.createRequest(searchQuery);
  }

  private JsonNode createInheritedRules(JsonNode childNode, Map<Long, ArchiveUnit> parentMap)
      throws JsonProcessingException {
    ArchiveUnit childUnit = JsonService.toArchiveUnit(childNode);
    InheritedRules childInheritedRules = createInheritedRules();
    Arrays.stream(RuleType.values())
        .forEach(ruleType -> addInheritedRule(childInheritedRules, childUnit, parentMap, ruleType));
    return JsonService.toJson(childInheritedRules);
  }

  private static void addInheritedRule(
      InheritedRules childInheritedRules,
      ArchiveUnit childUnit,
      Map<Long, ArchiveUnit> parentMap,
      RuleType ruleType) {

    Management childMgt = childUnit.getManagement();
    if (childMgt != null) {
      AbstractRules aRules = childMgt.getRules(ruleType);
      if (aRules != null && aRules.getRuleInheritance().getPreventInheritance() == Boolean.TRUE) {
        return;
      }
    }

    List<String> paths = new ArrayList<>();

    for (Long parentId : childUnit.getParentIds()) {
      ArchiveUnit parentUnit = parentMap.get(parentId);
      if (parentUnit == null) return;

      paths.add(parentId.toString());
      Management parentMgt = parentUnit.getManagement();
      if (parentMgt != null) {
        AbstractRules parentRules = parentMgt.getRules(ruleType);
        if (parentRules != null) {
          for (Rule parentRule : parentRules.getRules()) {
            InheritedRule inheritedRule =
                createInheritedRule(parentUnit, parentId, parentRule, paths);
            childInheritedRules.getRules(ruleType).getRules().add(inheritedRule);
          }
          if (parentRules instanceof FinalActionRule faRule) {
            InheritedProperty inheritedProperty =
                createInheritedProperty(parentUnit, parentId, faRule, paths);
            if (inheritedProperty != null) {
              childInheritedRules.getRules(ruleType).getProperties().add(inheritedProperty);
            }
          }
          if (parentRules.getRuleInheritance().getPreventInheritance() == Boolean.TRUE) return;
        }
      }
    }
  }

  private static InheritedRules createInheritedRules() {
    InheritedRules inheritedRules = new InheritedRules();
    inheritedRules.setAccessRules(new AccessInheritedRules());
    inheritedRules.setAppraisalRules(new AppraisalInheritedRules());
    inheritedRules.setDisseminationRules(new DisseminationInheritedRules());
    inheritedRules.setStorageRules(new StorageInheritedRules());
    inheritedRules.setClassificationRules(new ClassificationInheritedRules());
    inheritedRules.setReuseRules(new ReuseInheritedRules());
    inheritedRules.setHoldRules(new HoldInheritedRules());
    return inheritedRules;
  }

  private static InheritedRule createInheritedRule(
      ArchiveUnit parentUnit, Long parentId, Rule parentRule, List<String> paths) {
    InheritedRule inheritedRule = parentRule.createInheritedRule();
    inheritedRule.setUnitId(parentId.toString());
    inheritedRule.setOriginatingAgency(parentUnit.getServiceProducer());
    inheritedRule.getPaths().addAll(paths);
    return inheritedRule;
  }

  private static InheritedProperty createInheritedProperty(
      ArchiveUnit parentUnit, Long parentId, FinalActionRule parentRule, List<String> paths) {
    InheritedProperty inheritedProperty = null;
    String finalAction = parentRule.getFinalAction();
    if (finalAction != null) {
      inheritedProperty = new InheritedProperty();
      inheritedProperty.setUnitId(parentId.toString());
      inheritedProperty.setOriginatingAgency(parentUnit.getServiceProducer());
      inheritedProperty.getPaths().addAll(paths);
      inheritedProperty.setPropertyName("FinalAction");
      inheritedProperty.setPropertyValue(finalAction);
    }
    return inheritedProperty;
  }

  private static JsonNode getDefaultStorage() {
    ObjectNode storageNode = JsonNodeFactory.instance.objectNode();
    storageNode.put("strategyId", "default");
    return storageNode;
  }

  public Stream<JsonNode> searchStream(
      Long tenant, AccessContractDb accessContract, SearchQuery searchQuery, Converter converter)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(searchQuery, QUERY_MUST_BE_NOT_NULL);

    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);

    try {
      StreamRequest streamRequest = searchParser.createStreamRequest(searchQuery);
      return searchEngineService
          .searchStream(streamRequest, JsonNode.class)
          .map(converter::convert);
    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", searchQuery), ex);
    }
  }

  // Get Binary Object Metadata
  public SearchResult<JsonNode> getObjectMetadataByUnitId(
      Long tenant, AccessContractDb accessContract, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);
    Assert.isTrue(id >= 0, String.format(ARCHIVE_UNIT_ID_MUST_BE_POSITIVE, id));

    SearchQuery query = queryByUnitId(id);
    return searchUnits(tenant, accessContract, query, ObjectConverter.INSTANCE);
  }

  public StreamContent getBinaryObjectByUnitId(
      Long tenant, AccessContractDb accessContract, Long id, BinaryVersion binaryVersion)
      throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);

    SearchQuery query = queryByUnitId(id);
    JsonNode unit = getUnitByIdQuery(tenant, accessContract, query, id);
    ArchiveUnit archiveUnit = JsonService.toArchiveUnit(unit);

    List<Qualifiers> qualifiers = archiveUnit.getQualifiers();
    if (!qualifiers.isEmpty()) {
      ObjectVersion ov = Qualifiers.getObjectVersion(qualifiers, binaryVersion);
      if (ov != null) {
        String filename = StringUtils.defaultIfBlank(ov.getFilename(), "unknown");
        String mimetype = StringUtils.defaultIfBlank(ov.getMimeType(), "application/octet-stream");
        TenantDb tenantDb = tenantService.getTenantDb(tenant);
        try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
          return new StreamContent(
              filename,
              mimetype,
              storageDao.getBinaryObjectStream(
                  tenant,
                  tenantDb.getStorageOffers(),
                  ov.getOperationId(),
                  ov.getPos(),
                  ov.getId()));
        }
      }
    }

    throw new NotFoundException(
        "Binary Object not found",
        String.format(
            "Failed to find binary object with tenant: %s - accessContractId: %s - binary version: %s - id: %s",
            tenant, accessContract.getIdentifier(), binaryVersion, id));
  }

  public StreamContent getBinaryObjectByBinaryId(
      Long tenant, AccessContractDb accessContract, Long id) throws IOException {

    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.isTrue(id >= 0, String.format("BinaryObjectId '%s' cannot be negative", id));

    SearchQuery query = queryByBinaryId(id);
    JsonNode unit = getUnitByIdQuery(tenant, accessContract, query, id);
    ArchiveUnit archiveUnit = JsonService.toArchiveUnit(unit);

    // Fetch the binary object
    for (Qualifiers q : archiveUnit.getQualifiers()) {
      if (q.isBinaryQualifier()) {
        for (ObjectVersion ov : q.getVersions()) {
          if (id.equals(ov.getId())) {
            String filename = StringUtils.defaultIfBlank(ov.getFilename(), "unknown");
            String mimetype =
                StringUtils.defaultIfBlank(ov.getMimeType(), "application/octet-stream");
            TenantDb tenantDb = tenantService.getTenantDb(tenant);
            try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
              return new StreamContent(
                  filename,
                  mimetype,
                  storageDao.getBinaryObjectStream(
                      tenant, tenantDb.getStorageOffers(), ov.getOperationId(), ov.getPos(), id));
            }
          }
        }
      }
    }

    throw new NotFoundException(
        "Binary Object not found",
        String.format(
            "Failed to find binary object with tenant: %s - accessContractId: %s - id: %s",
            tenant, accessContract.getIdentifier(), id));
  }

  public List<JsonNode> getByIds(Long tenant, AccessContractDb accessContract, List<Long> ids)
      throws IOException {

    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ArrayNode valuesNode = idNode.putArray("#id");
    ids.stream().map(Object::toString).forEach(valuesNode::add);
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$in", idNode);
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();

    SearchQuery searchQuery =
        SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();

    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);

    SearchRequest searchRequest = searchParser.createRequest(searchQuery);
    log.info("Search JSON query: {}", JsonUtils.toJson(searchRequest));
    SearchResponse<JsonNode> response = searchEngineService.search(searchRequest, JsonNode.class);

    return response.hits().hits().stream()
        .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
        .filter(Objects::nonNull)
        .map(UnitConverter.INSTANCE::convert)
        .toList();
  }

  // "$eq": { "#id": id }
  private static SearchQuery queryByUnitId(Long id) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("#id", id.toString()));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    return SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
  }

  // "$eq": { "#object": id }
  private static SearchQuery queryByBinaryId(Long id) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("#object_id", id.toString()));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    return SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
  }

  private JsonNode getUnitByIdQuery(
      Long tenant, AccessContractDb accessContract, SearchQuery query, Long id) throws IOException {

    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser parser = SearchParser.create(tenant, accessContract, ontologyMapper);

    SearchRequest request = parser.createRequest(query);
    SearchResponse<JsonNode> response = searchEngineService.search(request, JsonNode.class);

    HitsMetadata<JsonNode> hits = response.hits();
    TotalHits total = hits.total();
    if (total == null || total.value() == 0) {
      throw new NotFoundException(
          "Archive Unit not found",
          String.format("Failed to find archive unit with id: %s - tenant: %s", id, tenant));
    }
    return hits.hits().getFirst().source();
  }

  // For internal use only. This request is looking for the document internal id
  // and is able to get the relevant document even when the document is not yet fully indexed.
  // EXT_KEYS are excluded because extended fields are present in the extends attribute of
  // ArchiveUnit
  public ArchiveUnit getLinkedArchiveUnit(Long tenant, Long id) throws IOException {
    GetRequest request =
        new GetRequest.Builder()
            .index(ArchiveUnitIndex.ALIAS)
            .id(String.valueOf(id))
            .sourceExcludes(EXT, UPS)
            .build();
    ArchiveUnit archiveUnit = searchEngineService.getById(request, ArchiveUnit.class);
    if (archiveUnit == null || !archiveUnit.getTenant().equals(tenant)) {
      throw new NotFoundException(
          "Archive Unit not found",
          String.format(
              "Failed to find archive unit with id: %s - index: %s - tenant: %s",
              request.id(), request.index(), tenant));
    }
    return archiveUnit;
  }

  // For internal use only
  public ArchiveUnit getLinkedArchiveUnit(Long tenant, NamedField namedField, FieldValue fieldValue)
      throws IOException {

    // Create query
    Field field = namedField.field();
    TermQuery keyQuery = TermQuery.of(t -> t.field(field.getFullName()).value(fieldValue));
    TermQuery tenantQuery =
        TermQuery.of(t -> t.field(TENANT_FIELD).value(v -> v.longValue(tenant)));
    BoolQuery boolQuery =
        BoolQuery.of(b -> b.must(keyQuery._toQuery()).filter(tenantQuery._toQuery()));

    SourceConfig sourceConfig = SourceConfig.of(s -> s.filter(f -> f.excludes(EXT).excludes(UPS)));

    // Create search request
    SearchRequest request =
        SearchRequest.of(
            r -> r.index(ArchiveUnitIndex.ALIAS).query(boolQuery._toQuery()).source(sourceConfig));

    SearchResponse<ArchiveUnit> response = searchEngineService.search(request, ArchiveUnit.class);

    int size = response.hits().hits().size();
    if (size == 1) {
      return response.hits().hits().getFirst().source();
    }

    if (size > 1) {
      throw new BadRequestException(
          "Too many archive units found",
          String.format("Found more that one archive unit with key: %s", field.getFullName()));
    }

    throw new NotFoundException(
        "Archive Unit not found",
        String.format("Failed to find archive unit with key: %s", field.getFullName()));
  }

  // For internal use only. This request is looking for the document internal
  // id and is able to get the relevant document even when the document is not yet fully indexed.
  // EXT_KEYS are excluded because extended fields are present in the extends attribute of
  // ArchiveUnit.  ids must be non-empty.
  public List<ArchiveUnit> getArchiveUnits(long tenant, List<Long> ids) throws IOException {
    // TODO Throw exception is list is emptu
    List<MultiGetOperation> mgetList =
        ids.stream()
            .map(Object::toString)
            .map(id -> new MultiGetOperation.Builder().id(id).build())
            .toList();
    MgetRequest request =
        new MgetRequest.Builder()
            .index(ArchiveUnitIndex.ALIAS)
            .sourceExcludes(EXT, UPS)
            .docs(mgetList)
            .build();
    return searchEngineService
        .getMultiById(request, ArchiveUnit.class)
        .filter(unit -> unit.getTenant() == tenant)
        .toList();
  }

  public String openPointInTime() throws IOException {
    return searchEngineService.openPointInTime(ArchiveUnitIndex.ALIAS);
  }

  public void closePointInTime(String pitId) throws IOException {
    searchEngineService.closePointInTime(pitId);
  }

  // Internal use only
  public Stream<ArchiveUnit> searchChildrenStream(
      Long tenant, AccessContractDb accessContract, Long id) throws IOException {
    return searchAllChildrenStream(tenant, accessContract, id, null);
  }

  // Internal use only
  public Stream<ArchiveUnit> searchAllChildrenStream(
      Long tenant, AccessContractDb accessContract, Long id, String pitId) throws IOException {
    StreamRequest searchRequest = createAllChildrenRequest(tenant, accessContract, id);
    return searchEngineService.searchStream(searchRequest, pitId, ArchiveUnit.class);
  }

  private StreamRequest createAllChildrenRequest(
      Long tenant, AccessContractDb accessContract, Long id) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$eq", idNode.put("#allunitups", id));
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    SearchQuery searchQuery =
        SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);
    return searchParser.createStreamRequest(searchQuery);
  }

  // Internal use only
  public Stream<ArchiveUnit> searchAllChildrenStream(
      Long tenant, AccessContractDb accessContract, List<Long> ids, String pitId)
      throws IOException {
    StreamRequest searchRequest = createAllChildrenRequest(tenant, accessContract, ids);
    return searchEngineService.searchStream(searchRequest, pitId, ArchiveUnit.class);
  }

  // TODO Throw exception is list is emptu
  private StreamRequest createAllChildrenRequest(
      Long tenant, AccessContractDb accessContract, List<Long> ids) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ArrayNode valuesNode = idNode.putArray("#allunitups");
    ids.stream().map(Object::toString).forEach(valuesNode::add);
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$in", idNode);
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    SearchQuery searchQuery =
        SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);
    return searchParser.createStreamRequest(searchQuery);
  }

  public Stream<ArchiveUnit> searchFirstChildrenStream(
      Long tenant, AccessContractDb accessContract, List<Long> ids, String pitId)
      throws IOException {
    StreamRequest searchRequest = createFirstChildrenRequest(tenant, accessContract, ids);
    return searchEngineService.searchStream(searchRequest, pitId, ArchiveUnit.class);
  }

  // TODO Throw exception is list is emptu
  private StreamRequest createFirstChildrenRequest(
      Long tenant, AccessContractDb accessContract, List<Long> ids) {
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    ArrayNode valuesNode = idNode.putArray("#unitup");
    ids.stream().map(Object::toString).forEach(valuesNode::add);
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$in", idNode);
    ObjectNode projectionNode = JsonNodeFactory.instance.objectNode();
    SearchQuery searchQuery =
        SearchQuery.builder().queryNode(queryNode).projectionNode(projectionNode).build();
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);
    SearchParser searchParser = SearchParser.create(tenant, accessContract, ontologyMapper);
    return searchParser.createStreamRequest(searchQuery);
  }

  public boolean existsById(Long tenant, Long id) throws IOException {
    try {
      getLinkedArchiveUnit(tenant, id);
      return true;
    } catch (NotFoundException ex) {
      return false;
    }
  }

  // Search LFC
  public JsonNode getUnitLifecycles(Long tenant, AccessContractDb accessContractDb, Long unitId)
      throws IOException {
    JsonNode node = getRawUnit(tenant, accessContractDb, unitId);
    return lifecycleConverterService.convertUnitLfc(tenant, node);
  }

  public JsonNode getObjectLifecycles(Long tenant, AccessContractDb accessContractDb, Long unitId)
      throws IOException {
    JsonNode node = getRawUnit(tenant, accessContractDb, unitId);
    return lifecycleConverterService.convertObjectLfc(tenant, node);
  }
}
