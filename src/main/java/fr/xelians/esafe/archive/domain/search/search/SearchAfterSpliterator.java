/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.search.service.SearchEngineService;
import java.io.IOException;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import org.apache.commons.lang.StringUtils;

// Inspired from
// https://stackoverflow.com/questions/23462209/stream-api-and-queues-subscribe-to-blockingqueue-stream-style
public class SearchAfterSpliterator<T> implements Spliterator<T> {

  private final ElasticsearchClient esClient;
  private final Class<T> docClass;
  private final StreamRequest request;
  private final String pitId;

  private int currentIndex;
  private int hitSize;
  private List<FieldValue> hitSort;
  private List<T> objects;
  private boolean finished = false;
  private boolean closePit = false;

  public SearchAfterSpliterator(
      ElasticsearchClient esClient, StreamRequest request, Class<T> docClass) throws IOException {
    this(esClient, request, null, docClass);
  }

  public SearchAfterSpliterator(
      ElasticsearchClient esClient, StreamRequest request, String pitId, Class<T> docClass)
      throws IOException {
    this.esClient = esClient;
    this.docClass = docClass;
    this.request = request;
    this.pitId = StringUtils.isBlank(pitId) ? openPointInTime() : pitId;
  }

  private String openPointInTime() throws IOException {
    closePit = true;
    return esClient
        .openPointInTime(fn -> fn.index(request.index()).keepAlive(t -> t.time("5m")))
        .id();
  }

  @Override
  public int characteristics() {
    return Spliterator.CONCURRENT | Spliterator.NONNULL | Spliterator.ORDERED;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public boolean tryAdvance(final Consumer<? super T> action) {
    if (finished) {
      return false;
    }

    T next = searchAfter();
    if (next == null) {
      finished = true;
      closePointInTime();
      return false;
    }

    action.accept(next);
    return true;
  }

  @Override
  public Spliterator<T> trySplit() {
    return null;
  }

  private T searchAfter() {
    try {
      if (objects == null) {
        return search(request.firstRequest(pitId));

      } else if (currentIndex == hitSize) {
        if (hitSize >= SearchEngineService.MAX_RESULT) {
          return search(request.nextRequest(pitId, hitSort));
        }
        return null;
      }
      return objects.get(currentIndex++);
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private T search(SearchRequest searchRequest) throws IOException {
    SearchResponse<T> response = esClient.search(searchRequest, docClass);
    List<Hit<T>> hits = response.hits().hits();
    hitSize = hits.size();
    if (hitSize > 0) {
      hitSort = hits.get(hitSize - 1).sort();
      objects = hits.stream().map(Hit::source).toList();
      currentIndex = 1;
      return objects.get(0);
    }
    return null;
  }

  private void closePointInTime() {
    try {
      if (pitId != null && closePit) {
        esClient.closePointInTime(p -> p.id(pitId));
      }
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }
}
