/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.referential.domain.search.Request;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor
public class CustomReferentialRepositoryImpl implements CustomReferentialRepository {

  @Override
  public <T> SearchResult<T> search(Request<T> request, SearchQuery query) {
    int offset = request.mainQuery().getFirstResult();
    int limit = request.mainQuery().getMaxResults();

    Long total = request.countQuery().getSingleResult();
    if (total == 0) {
      Hits hits = new Hits(offset, limit, 0, total);
      return new SearchResult<>(HttpStatus.OK.value(), hits, Collections.emptyList(), null, query);
    }

    List<T> results = request.mainQuery().getResultList();
    Hits hits = new Hits(offset, limit, results.size(), total);
    return new SearchResult<>(HttpStatus.OK.value(), hits, results, null, query);
  }

  @Override
  public SearchResult<JsonNode> search(
      Request<Object[]> request, SearchQuery query, List<String> projections) {

    int offset = request.mainQuery().getFirstResult();
    int limit = request.mainQuery().getMaxResults();
    long total = request.countQuery().getSingleResult();
    if (total == 0) {
      Hits hits = new Hits(offset, limit, 0, total);
      return new SearchResult<>(HttpStatus.OK.value(), hits, Collections.emptyList(), null, query);
    }

    List<Object[]> results = request.mainQuery().getResultList();
    List<JsonNode> nodes = results.stream().map(r -> toJson(projections, r)).toList();

    Hits hits = new Hits(offset, limit, results.size(), total);
    return new SearchResult<>(HttpStatus.OK.value(), hits, nodes, null, query);
  }

  private JsonNode toJson(List<String> projections, Object[] result) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    checkSize(projections.size(), result.length);
    for (var i = 0; i < projections.size(); i++) {
      String fieldName = projections.get(i);
      // Ideally the json conversion should be done at the service level and
      // use the projection field type to get the relevant type
      if ("tenant".equals(fieldName)) {
        node.put("#tenant", (Long) result[i]);
      } else {
        node.put(fieldName, result[i].toString());
      }
    }
    return node;
  }

  private void checkSize(int size, int length) {
    if (size != length) {
      throw new InternalException(
          "Failed to create json from referential request",
          String.format("Projection size '%s' and detail size '%s' are not equal", size, length));
    }
  }
}
