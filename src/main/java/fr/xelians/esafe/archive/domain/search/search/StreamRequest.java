/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import fr.xelians.esafe.search.service.SearchEngineService;
import java.util.ArrayList;
import java.util.List;

public class StreamRequest {

  private final SearchRequest request;
  private final List<SortOptions> sortOptions;

  public StreamRequest(SearchRequest request) {
    this.request = request;
    this.sortOptions = createSortOptions();
  }

  private SearchRequest.Builder createBuilder(String pitId) {
    return new SearchRequest.Builder()
        .query(request.query())
        .size(SearchEngineService.MAX_RESULT)
        .sort(sortOptions)
        .source(request.source())
        .pit(p -> p.id(pitId))
        .trackTotalHits(t -> t.enabled(false));
  }

  public SearchRequest firstRequest(String pitId) {
    return createBuilder(pitId).build();
  }

  public SearchRequest nextRequest(String pitId, List<FieldValue> hitSort) {
    return createBuilder(pitId).searchAfter(hitSort).build();
  }

  private List<SortOptions> createSortOptions() {
    List<SortOptions> so = new ArrayList<>(request.sort());
    so.add(SortOptions.of(s -> s.field(f -> f.field("_shard_doc").order(SortOrder.Asc))));
    return so;
  }

  public List<String> index() {
    return request.index();
  }
}
