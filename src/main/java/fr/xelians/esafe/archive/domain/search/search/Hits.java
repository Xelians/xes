/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.search;

import static co.elastic.clients.elasticsearch.core.search.TotalHitsRelation.Eq;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.NumUtils;

/*
 * @author Emmanuel Deviller
 */
public record Hits(
    @JsonProperty("offset") long offset,
    @JsonProperty("limit") long limit,
    @JsonProperty("size") long size,
    @JsonProperty("total") long total) {

  public static Hits create(SearchRequest request, HitsMetadata<?> hitsMeta) {
    TotalHits totalHits = hitsMeta.total();
    long total = totalHits == null ? 0 : getTotal(totalHits);
    long offset = NumUtils.toLong(request.from(), 0);
    long limit = NumUtils.toLong(request.size(), total + 1);
    return new Hits(offset, limit, hitsMeta.hits().size(), total);
  }

  private static Long getTotal(TotalHits totalHits) {
    return totalHits.relation() == Eq ? totalHits.value() : totalHits.value() + 1;
  }
}
