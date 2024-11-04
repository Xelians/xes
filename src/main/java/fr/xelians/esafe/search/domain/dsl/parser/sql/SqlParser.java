/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.parser.sql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.search.domain.dsl.operator.*;
import fr.xelians.esafe.search.domain.dsl.operator.sql.*;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.index.Searchable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.util.*;
import java.util.Map.Entry;
import lombok.Getter;

/**
 * Structured Query Language Parser *
 *
 * @author Emmanuel Deviller
 */
@Getter
public abstract class SqlParser<T> extends DslParser<Predicate> {

  protected static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  protected static final String QUERY_IS_EMPTY_OR_NOT_DEFINED = "Query is empty or not defined";
  protected static final String PROJECTION_IS_NOT_DEFINED = "Projection is not defined";

  protected static final JsonNode QUERY_ALL = queryAll();

  protected static final String TENANT_FIELD = "tenant";
  protected static final String ORDER_BY_FIELD = "$orderby";

  protected final Long tenant;
  protected final Class<T> entityClass;
  protected final EntityManager entityManager;
  protected final CriteriaBuilder criteriaBuilder;

  protected Root<T> rootEntity;

  protected SqlParser(
      Searchable searchable, Long tenant, Class<T> entityClass, EntityManager entityManager) {
    super(searchable);
    this.tenant = tenant;
    this.entityClass = entityClass;
    this.entityManager = entityManager;
    this.criteriaBuilder = entityManager.getCriteriaBuilder();
  }

  public OntologyMapper getOntologyMapper() {
    return null;
  }

  protected Predicate wherePredicate(SearchContext searchContext, JsonNode node) {
    if (node.isArray()) {
      if (node.size() > 1) {
        throwBadRequestException("Multiple root queries are not allowed");
      }
      node = node.get(0);
    }

    Operator operator = null;

    // Parse the first level of the query
    for (Iterator<Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
      Entry<String, JsonNode> entry = ite.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();

      if (operator == null) {
        operator = createOperator(searchContext, key, value);
      } else {
        throwBadRequestException("Multiple root operators are not allowed");
      }
    }

    if (operator == null) {
      throwBadRequestException("Failed to find root operator in DSL query ");
    }

    Predicate tenantPredicate = criteriaBuilder.equal(rootEntity.get(TENANT_FIELD), tenant);
    return criteriaBuilder.and(tenantPredicate, create(operator));
  }

  protected Operator createOperator(SearchContext searchContext, String operator, JsonNode node) {

    if (operator == null) {
      throwBadRequestException("Failed to create undefined operator");
    }

    if (!operator.startsWith("$")) {
      throwBadRequestException(
          String.format("Failed to create operator '%s' that does not start with $", operator));
    }

    // Switch may be faster than HashMap, so keep it simple stupid...
    return switch (operator) {
      case "$and" -> new And(this, searchContext, node);
      case "$eq" -> new Eq(this, searchContext, node);
      case "$gt" -> new Gt(this, searchContext, node);
      case "$gte" -> new Gte(this, searchContext, node);
      case "$in" -> new In(this, searchContext, node);
      case "$lt" -> new Lt(this, searchContext, node);
      case "$lte" -> new Lte(this, searchContext, node);
      case "$match" -> new Match(this, searchContext, node);
      case "$match_all" -> new MatchAll(this, searchContext, node);
      case "$match_phrase" -> new MatchPhrase(this, searchContext, node);
      case "$match_phrase_prefix" -> new MatchPhrasePrefix(this, searchContext, node);
      case "$neq" -> new Neq(this, searchContext, node);
      case "$nin" -> new Nin(this, searchContext, node);
      case "$not" -> new Not(this, searchContext, node);
      case "$or" -> new Or(this, searchContext, node);
      default -> throw new BadRequestException(
          CREATION_FAILED,
          String.format("Failed to parse DSL with unknown operator '%s'", operator));
    };
  }

  protected List<Order> createSortOrders(SearchContext searchContext, JsonNode node) {
    if (node == null) {
      return Collections.emptyList();
    }
    if (!node.isObject()) {
      throwBadRequestException("Failed to process sort $query - not an object");
    }

    List<Order> orders = new ArrayList<>();
    JsonNode fieldsNode = node.get(ORDER_BY_FIELD);
    if (fieldsNode != null) {
      if (!fieldsNode.isObject()) {
        throwBadRequestException("Failed to process sort $orderby - not an object");
      }
      fieldsNode
          .fields()
          .forEachRemaining(
              entry -> {
                int value = entry.getValue().asInt();
                if (value != 0) {
                  String fieldName =
                      getQueryField(searchContext.getDocType(), entry.getKey()).getFullName();
                  Order order =
                      value > 0
                          ? criteriaBuilder.asc(rootEntity.get(fieldName))
                          : criteriaBuilder.desc(rootEntity.get(fieldName));
                  orders.add(order);
                }
              });
    }

    return orders;
  }

  protected static boolean isEmpty(JsonNode node) {
    return node == null || node.isEmpty();
  }

  protected static JsonNode getNonEmptyQuery(SearchQuery searchQuery) {
    return isEmpty(searchQuery.queryNode()) ? QUERY_ALL : searchQuery.queryNode();
  }

  private static JsonNode queryAll() {
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    ObjectNode idNode = JsonNodeFactory.instance.objectNode();
    queryNode.set("$neq", idNode.put("#id", -1));
    return queryNode;
  }
}
