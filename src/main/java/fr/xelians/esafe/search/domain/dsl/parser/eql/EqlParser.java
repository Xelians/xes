/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.parser.eql;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.util.NamedValue;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.search.domain.dsl.operator.*;
import fr.xelians.esafe.search.domain.dsl.operator.eql.*;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.index.Searchable;
import java.util.*;
import java.util.Map.Entry;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

/** Elastic Query Language Parser */
public abstract class EqlParser extends DslParser<Query> {

  // TODO MAX_FACETS & LIMIT_MAX must be configurable - 1000 is the default
  protected static final int MAX_FACETS = 12;
  protected static final String TENANT_FIELD = "_tenant";
  protected static final String ORDER_BY_FIELD = "$orderby";

  @Getter protected final Long tenant;

  protected EqlParser(Searchable searchable, Long tenant) {
    super(searchable);
    this.tenant = tenant;
  }

  protected List<Query> createFilterQueries() {
    List<Query> filterQueries = new ArrayList<>();

    // Check Tenant
    TermQuery tenantQuery =
        TermQuery.of(t -> t.field(TENANT_FIELD).value(v -> v.longValue(tenant)));
    filterQueries.add(tenantQuery._toQuery());

    return filterQueries;
  }

  protected RootQuery createRootQuery(SearchContext searchContext, JsonNode node) {
    if (node.isArray()) {
      if (node.size() > 1) {
        throwBadRequestException("Multiple root queries are not allowed");
      }
      node = node.get(0);
    }

    Operator operator = null;
    int depth = Integer.MAX_VALUE;

    // Parse the first level of the query
    for (Iterator<Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
      Entry<String, JsonNode> entry = ite.next();
      String key = entry.getKey();
      JsonNode value = entry.getValue();

      if ("$depth".equals(key)) {
        depth = value.asInt();
      } else if (operator == null) {
        operator = createOperator(searchContext, key, value);
      } else {
        throwBadRequestException("Multiple root operators are not allowed");
      }
    }

    if (operator == null) {
      throwBadRequestException("Failed to find root operator in DSL query ");
    }

    return new RootQuery(create(operator), depth);
  }

  protected Operator createOperator(SearchContext searchContext, String operator, JsonNode node) {

    if (operator == null) {
      throwBadRequestException("Failed to create not defined operator");
    }

    if (!operator.startsWith("$")) {
      throwBadRequestException(
          String.format("Failed to create operator '%s' that does not start with $", operator));
    }

    // Switch may be faster than HashMap, so keep it simple stupid...
    return switch (operator) {
      case "$and" -> new And(this, searchContext, node);
      case "$eq" -> new Eq(this, searchContext, node);
      case "$exists" -> new Exists(this, searchContext, node);
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
      case "$range" -> new Range(this, searchContext, node);
      case "$regex" -> new Regex(this, searchContext, node);
      case "$search" -> new Search(this, searchContext, node);
      case "$wildcard" -> new Wildcard(this, searchContext, node);
      default -> throw new BadRequestException(
          CREATION_FAILED,
          String.format("Failed to parse DSL with unknown operator '%s'", operator));
    };
  }

  protected List<SortOptions> createSortOptions(SearchContext searchContext, JsonNode node) {
    if (node == null) {
      return Collections.emptyList();
    }
    if (!node.isObject()) {
      throwBadRequestException("Failed to process sort $query - not an object");
    }

    List<SortOptions> sortOptions = new ArrayList<>();
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
                  SortOrder order = value > 0 ? SortOrder.Asc : SortOrder.Desc;
                  sortOptions.add(
                      SortOptions.of(s -> s.field(f -> f.field(fieldName).order(order))));
                }
              });
    }
    return sortOptions;
  }

  protected Map<String, Aggregation> createAggregations(
      SearchContext searchContext, JsonNode node) {
    if (node == null) {
      return Collections.emptyMap();
    }
    if (!node.isArray()) {
      throwBadRequestException("Failed to process $facets - not an array");
    }

    Map<String, Aggregation> aggMap = new HashMap<>();
    int count = 0;
    for (JsonNode elt : node) {
      String name = JsonUtils.asText(elt.get("$name"));
      if (name == null) {
        throwBadRequestException("Failed to process $facets - name is not defined");
      }
      for (Iterator<Entry<String, JsonNode>> ite = elt.fields(); ite.hasNext(); ) {
        Entry<String, JsonNode> entry = ite.next();
        AggCount ac = selectAggregation(searchContext, entry, name);
        if (ac.count() > 0) {
          count += ac.count();
          if (count > MAX_FACETS) {
            throwBadRequestException(
                String.format(
                    "Too many facets/buckets: '%s' - Max allowed is '%s'", count, MAX_FACETS));
          }
          aggMap.put(name, ac.aggregation());
        }
      }
    }
    return aggMap;
  }

  private AggCount selectAggregation(
      SearchContext searchContext, Entry<String, JsonNode> entry, String name) {

    return switch (entry.getKey()) {
      case "$name" -> new AggCount(null, 0);
      case "$terms" -> createTermsAggregation(searchContext, entry.getValue());
      case "$date_range" -> createDateRangeAggregation(searchContext, entry.getValue());
      case "$filters" -> createFiltersAggregation(searchContext, entry.getValue());
      default -> throw new BadRequestException(
          CREATION_FAILED, String.format("Failed to process $facets - unknown field '%s'", name));
    };
  }

  protected AggCount createTermsAggregation(SearchContext searchContext, JsonNode node) {
    String field = null;
    Integer size = null;
    SortOrder sortOrder = null;

    for (Iterator<Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
      Entry<String, JsonNode> entry = ite.next();
      switch (entry.getKey()) {
        case "$field" -> field =
            getQueryField(searchContext.getDocType(), entry.getValue().asText()).getFullName();
        case "$size" -> size = entry.getValue().asInt();
        case "$order" -> sortOrder =
            SortOrder.valueOf(StringUtils.capitalize(entry.getValue().asText().toLowerCase()));
        default -> throwBadRequestException(
            String.format("Failed to process $facets - unknown field '%s'", entry.getKey()));
      }
    }

    if (field == null) {
      throwBadRequestException("Failed to process $facets - field is not defined");
    }

    TermsAggregation.Builder builder = new TermsAggregation.Builder().field(field).size(size);
    // We can sort on the _key or on the _count
    if (sortOrder != null) {
      builder.order(new NamedValue<>("_count", sortOrder));
    }

    Aggregation aggregation = builder.build()._toAggregation();
    return new AggCount(aggregation, 1);
  }

  protected AggCount createDateRangeAggregation(SearchContext searchContext, JsonNode node) {
    String field = null;
    String format = null;
    final List<DateRangeExpression> ranges = new ArrayList<>();

    for (Iterator<Entry<String, JsonNode>> ite = node.fields(); ite.hasNext(); ) {
      Entry<String, JsonNode> entry = ite.next();
      switch (entry.getKey()) {
        case "$field" -> field =
            getQueryField(searchContext.getDocType(), entry.getValue().asText()).getFullName();
        case "$format" -> format = entry.getValue().asText();
        case "$ranges" -> entry.getValue().forEach(e -> ranges.add(createDateRangeExpr(e)));
        default -> throwBadRequestException(
            String.format(
                "Failed to process date range facet - unknown field '%s'", entry.getKey()));
      }
    }

    if (field == null) {
      throwBadRequestException("Failed to process facet - field is not defined");
    }

    Aggregation aggregation =
        new DateRangeAggregation.Builder()
            .field(field)
            .format(format)
            .ranges(ranges)
            .build()
            ._toAggregation();
    return new AggCount(aggregation, ranges.size());
  }

  protected static DateRangeExpression createDateRangeExpr(JsonNode elt) {
    DateRangeExpression.Builder dreb = new DateRangeExpression.Builder();
    for (Iterator<Entry<String, JsonNode>> ite = elt.fields(); ite.hasNext(); ) {
      Entry<String, JsonNode> entry = ite.next();
      switch (entry.getKey()) {
        case "$from" -> dreb.from(f -> f.expr(entry.getValue().asText()));
        case "$to" -> dreb.to(f -> f.expr(entry.getValue().asText()));
        default -> throwBadRequestException(
            String.format(
                "Failed to process date range expression facet - unknown field '%s'",
                entry.getKey()));
      }
    }
    return dreb.build();
  }

  protected AggCount createFiltersAggregation(SearchContext searchContext, JsonNode node) {
    Map<String, Query> queries = new HashMap<>();

    if (node != null) {
      JsonNode filtersNode = node.get("$query_filters");
      if (filtersNode == null || !filtersNode.isArray()) {
        throwBadRequestException("Failed to process $facets - not an array");
      }
      for (JsonNode elt : filtersNode) {
        RootQuery rootQuery = null;
        String name = null;
        for (Iterator<Entry<String, JsonNode>> ite = elt.fields(); ite.hasNext(); ) {
          Entry<String, JsonNode> entry = ite.next();
          switch (entry.getKey()) {
            case "$query" -> rootQuery = createRootQuery(searchContext, entry.getValue());
            case "$name" -> name = entry.getValue().asText();
            default -> throwBadRequestException(
                String.format("Failed to process $facets - unknown field '%s'", entry.getKey()));
          }
        }
        if (rootQuery == null) {
          throwBadRequestException("Failed to process $facets - query is not defined");
        }
        queries.put(name, rootQuery.query());
      }
    }
    Buckets<Query> bq = Buckets.of(b -> b.keyed(queries));
    Aggregation aggregation = FiltersAggregation.of(f -> f.filters(bq))._toAggregation();
    return new AggCount(aggregation, queries.size());
  }

  protected static boolean isEmpty(JsonNode node) {
    return node == null || node.isEmpty();
  }
}
