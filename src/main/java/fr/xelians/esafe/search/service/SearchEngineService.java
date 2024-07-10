/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ExpandWildcard;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import fr.xelians.esafe.archive.domain.search.search.SearchAfterSpliterator;
import fr.xelians.esafe.archive.domain.search.search.StreamRequest;
import fr.xelians.esafe.common.entity.searchengine.DocumentSe;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.search.domain.dsl.bucket.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchEngineService {

  // Must be equal to index.max_result_window (10_000 is the default)
  public static final int MAX_RESULT = 10_000;
  public static final String UNKNOWN_REASON = "Unknown reason";
  private final ElasticsearchClient esClient;

  public boolean existsById(ExistsRequest existsRequest) throws IOException {
    BooleanResponse response = esClient.exists(existsRequest);
    return response.value();
  }

  public <T extends DocumentSe> T getById(GetRequest getRequest, Class<T> klass)
      throws IOException {
    return esClient.get(getRequest, klass).source();
  }

  public <T extends DocumentSe> Stream<T> getMultiById(MgetRequest mgetRequest, Class<T> klass)
      throws IOException {
    MgetResponse<T> response = esClient.mget(mgetRequest, klass);
    return response.docs().stream().map(item -> item.result().source());
  }

  public <T> SearchResponse<T> search(SearchRequest searchRequest, Class<T> docClass)
      throws IOException {
    return esClient.search(searchRequest, docClass);
  }

  public <T> Stream<T> searchStream(StreamRequest request, Class<T> docClass) throws IOException {
    return StreamSupport.stream(new SearchAfterSpliterator<>(esClient, request, docClass), false);
  }

  // It is the responsibility of the client to close the Pit (if any)
  public <T> Stream<T> searchStream(StreamRequest request, String pitId, Class<T> docClass)
      throws IOException {
    return StreamSupport.stream(
        new SearchAfterSpliterator<>(esClient, request, pitId, docClass), false);
  }

  public String openPointInTime(String indexName) throws IOException {
    return esClient.openPointInTime(fn -> fn.index(indexName).keepAlive(t -> t.time("60m"))).id();
  }

  public void closePointInTime(String pitId) throws IOException {
    if (pitId != null) {
      esClient.closePointInTime(p -> p.id(pitId));
    }
  }

  public static <T, R> List<R> getResults(HitsMetadata<T> hits, Function<? super T, R> mapper) {
    return hits.hits().stream()
        .map(co.elastic.clients.elasticsearch.core.search.Hit::source)
        .map(mapper)
        .toList();
  }

  public static List<Facet> getFacets(Map<String, Aggregate> aggregations) {
    return aggregations.entrySet().stream()
        .map(e -> new Facet(e.getKey(), createBuckets(e.getValue())))
        .toList();
  }

  private static List<Bucket> createBuckets(Aggregate aggregate) {
    if (aggregate.isSterms()) {
      return aggregate.sterms().buckets().array().stream()
          .map(bk -> new Bucket(bk.key().stringValue(), bk.docCount()))
          .toList();
    } else if (aggregate.isLterms()) {
      return aggregate.lterms().buckets().array().stream()
          .map(bk -> new Bucket(String.valueOf(bk.key()), bk.docCount()))
          .toList();
    } else if (aggregate.isDterms()) {
      return aggregate.dterms().buckets().array().stream()
          .map(bk -> new Bucket(String.valueOf(bk.key()), bk.docCount()))
          .toList();
    } else if (aggregate.isDateRange()) {
      return aggregate.dateRange().buckets().array().stream()
          .map(bk -> new Bucket(bk.key(), bk.docCount()))
          .toList();
    } else if (aggregate.isFilters()) {
      return aggregate.filters().buckets().keyed().entrySet().stream()
          .map(e -> new Bucket(e.getKey(), e.getValue().docCount()))
          .toList();
    }
    throw new InternalException(
        "Failed to create facet buckets", String.format("Unknown '%s' aggregate", aggregate));
  }

  // Document methods
  public <T extends DocumentSe> void index(String name, T doc) throws IOException {
    esClient.index(b -> b.index(name).id(String.valueOf(doc.getId())).document(doc));
  }

  public <T extends DocumentSe> void bulkIndex(String name, List<T> docs) throws IOException {
    List<BulkOperation> bulkList =
        docs.stream()
            .map(
                doc ->
                    new BulkOperation.Builder()
                        .index(b -> b.index(name).id(String.valueOf(doc.getId())).document(doc))
                        .build())
            .toList();

    BulkResponse response = esClient.bulk(b -> b.operations(bulkList));
    if (response.errors()) {
      String reason =
          response.items().stream()
              .map(BulkResponseItem::error)
              .filter(Objects::nonNull)
              .map(e -> Objects.toString(e.reason(), UNKNOWN_REASON))
              .findFirst()
              .orElse(UNKNOWN_REASON);
      throw new IOException(
          String.format(
              "Failed to bulk index documents in index '%s' - Caused by: %s", name, reason));
    }
  }

  public <T extends DocumentSe> void bulkIndexRefresh(String name, List<T> docs)
      throws IOException {
    List<BulkOperation> bulkList =
        docs.stream()
            .map(
                doc ->
                    new BulkOperation.Builder()
                        .index(b -> b.index(name).id(String.valueOf(doc.getId())).document(doc))
                        .build())
            .toList();

    BulkResponse response = esClient.bulk(b -> b.operations(bulkList).refresh(Refresh.True));
    if (response.errors()) {
      String reason =
          response.items().stream()
              .map(BulkResponseItem::error)
              .filter(Objects::nonNull)
              .map(e -> Objects.toString(e.reason(), UNKNOWN_REASON))
              .findFirst()
              .orElse(UNKNOWN_REASON);
      throw new IOException(
          String.format(
              "Failed to bulk index documents in index '%s' - Caused by: %s", name, reason));
    }
  }

  public void bulkDelete(String name, List<Long> ids) throws IOException {
    List<BulkOperation> bulkList =
        ids.stream()
            .map(
                id ->
                    new BulkOperation.Builder()
                        .delete(d -> d.index(name).id(id.toString()))
                        .build())
            .toList();

    BulkResponse response = esClient.bulk(b -> b.operations(bulkList));
    if (response.errors()) {
      String reason =
          response.items().stream()
              .map(BulkResponseItem::error)
              .filter(Objects::nonNull)
              .map(ErrorCause::reason)
              .findFirst()
              .orElse(UNKNOWN_REASON);
      throw new IOException(
          String.format(
              "Failed to bulk delete documents in index '%s' - Caused by: %s", name, reason));
    }
  }

  public void bulkDeleteRefresh(String name, List<Long> ids) throws IOException {
    List<BulkOperation> bulkList =
        ids.stream()
            .map(
                id ->
                    new BulkOperation.Builder()
                        .delete(d -> d.index(name).id(id.toString()))
                        .build())
            .toList();

    BulkResponse response = esClient.bulk(b -> b.operations(bulkList).refresh(Refresh.True));
    if (response.errors()) {
      String reason =
          response.items().stream()
              .map(BulkResponseItem::error)
              .filter(Objects::nonNull)
              .map(e -> Objects.toString(e.reason(), UNKNOWN_REASON))
              .findFirst()
              .orElse(UNKNOWN_REASON);
      throw new IOException(
          String.format(
              "Failed to bulk delete documents in index '%s' - Caused by: %s", name, reason));
    }
  }

  // Index methods
  public void createIndex(String name, String alias, String mapping) throws IOException {
    log.info("Create index {} with alias {} ", name, alias);

    if (indexExists(name)) {
      throw new IOException(String.format("Failed to create already existent index '%s'", name));
    }

    BooleanResponse aliasExists = esClient.indices().existsAlias(e -> e.name(alias));
    if (aliasExists.value()) {
      throw new IOException(
          String.format(
              "Failed to create index '%s' with already existent alias '%s'", name, alias));
    }

    // Create the index
    CreateIndexResponse createIndex =
        esClient
            .indices()
            .create(ib -> ib.index(name).mappings(mb -> mb.withJson(new StringReader(mapping))));
    // .settings(s -> s.numberOfShards("2").numberOfReplicas("2"))
    // .aliases(alias, ab -> ab.isWriteIndex(true))
    if (!createIndex.acknowledged()) {
      throw new IOException(
          String.format("Failed to create index '%s with alias '%s'", name, alias));
    }

    // Add alias to new Index (we add the alias after creating the index to avoid weird error)
    PutAliasResponse putAlias =
        esClient.indices().putAlias(p -> p.index(name).name(alias).isWriteIndex(true));
    if (!putAlias.acknowledged()) {
      throw new IOException(
          String.format("Failed to assign alias '%s' to index '%s'", alias, name));
    }

    log.info("Create index {} done", createIndex.index());
  }

  public boolean indexExists(String name) throws IOException {
    return esClient.indices().exists(e -> e.index(name)).value();
  }

  public void deleteIndexWithPrefix(String prefix) throws IOException {
    //  TODO be sure to delete only user index
    //  GetIndexResponse getIndex = esClient.indices().get(g -> g.index("_all"));
    GetIndexResponse getIndex =
        esClient.indices().get(g -> g.index(prefix + "*").expandWildcards(ExpandWildcard.All));

    List<String> indexList = new ArrayList<>(getIndex.result().keySet());
    if (!indexList.isEmpty()) {
      DeleteIndexResponse deleteIndex = esClient.indices().delete(d -> d.index(indexList));
      if (!deleteIndex.acknowledged()) {
        throw new IOException(String.format("Failed to delete all index %s", indexList));
      }
      log.info("deleteIndexWithPrefix() - Index {} - {} deleted", prefix, indexList);
    } else {
      log.info("deleteIndexWithPrefix() - Index {} is empty", prefix);
    }
  }

  public void deleteIndex(String name) throws IOException {

    if (indexExists(name)) {
      DeleteIndexResponse deleteIndex = esClient.indices().delete(d -> d.index(name));
      if (!deleteIndex.acknowledged()) {
        throw new IOException(String.format("Failed to delete index %s", name));
      }
      log.info("deleteIndex() - Index {} deleted", name);
    } else {
      log.info("deleteIndex() - Index {} does not exist", name);
    }
  }

  public boolean existsAlias(String alias) throws IOException {
    BooleanResponse aliasExists = esClient.indices().existsAlias(e -> e.name(alias));
    if (aliasExists.value()) {
      GetAliasResponse getAlias = esClient.indices().getAlias(g -> g.name(alias));
      Set<String> indexSet = getAlias.result().keySet();
      String index = String.join(" ", indexSet);
      if (indexSet.size() > 1) {
        throw new IOException(
            String.format("Index alias %s references more than one index %s", alias, index));
      }
      log.info("Index alias {} references index {}", alias, String.join(" ", index));
      return true;
    }
    return false;
  }

  public void removeAlias(String alias) throws IOException {
    log.info("removeAlias() - Alias: {}", alias);
    BooleanResponse aliasExists = esClient.indices().existsAlias(e -> e.name(alias));
    if (aliasExists.value()) {
      // Get current index from alias
      GetAliasResponse getAlias = esClient.indices().getAlias(g -> g.name(alias));
      List<String> indexList = new ArrayList<>(getAlias.result().keySet());

      // Close old index
      CloseIndexResponse closeIndex = esClient.indices().close(c -> c.index(indexList));
      if (!closeIndex.acknowledged()) {
        throw new IOException(
            String.format("Failed to close index '%s' with index '%s'", alias, indexList));
      }
      log.info("removeAlias() - Old index {} closed", indexList);

      // Remove alias from index
      DeleteAliasResponse deleteAlias =
          esClient.indices().deleteAlias(d -> d.index(indexList).name(alias));
      if (!deleteAlias.acknowledged()) {
        throw new IOException(
            String.format("Failed to delete alias '%s' from index '%s'", alias, indexList));
      }
      log.info("removeAlias() - Alias: {} removed", alias);
    } else {
      log.info("removeAlias() - Alias: {} does not exist", alias);
    }
  }

  public void moveAliasTo(String alias, String name) throws IOException {
    log.info("moveAliasTo() - Alias: {} - Index: {}", alias, name);

    removeAlias(alias);

    // Add alias to new Index
    PutAliasResponse putAliasResponse =
        esClient.indices().putAlias(p -> p.index(name).name(alias).isWriteIndex(true));
    if (!putAliasResponse.acknowledged()) {
      throw new IOException(
          String.format("Failed to assign alias '%s' to index '%s'", alias, name));
    }

    log.info("moveAliasTo() - add Alias {} to Index {} done", alias, name);
  }

  // Any index changes, such as indexing or deleting documents, are written to disk during a Lucene
  // commit. However, Lucene commits are expensive operations, so they cannot be performed after
  // every change to the index.
  // In ES, each shard records every indexing operation in a transaction log called translog that
  // is fsynced after every request. When a document is indexed, it is added to the memory buffer
  // and written in the translog. Periodically, a flush performs a Lucene commit and reset the
  // translog. Thus, a translog contains all non flushed operations that could be replayed
  // in case of a crash. Note. The translog is an ES feature and is not part of Lucene.

  // A refresh causes documents in the memory buffer to be converted into an in-memory
  // segment which then becomes searchable. Refresh does not commit the data to make it durable.
  public void refresh(String name) throws IOException {
    esClient.indices().refresh(r -> r.index(name));
  }

  // An ES flush performs a Lucene commit, which includes writing the memory segments to disk
  // using fsync, then purging the old translog and starting a new translog.
  public void flush(String name) throws IOException {
    esClient.indices().flush(r -> r.index(name));
  }

  public Set<String> getIndicesByAlias(String alias) throws IOException {
    try {
      GetAliasResponse aliasResponse = esClient.indices().getAlias(g -> g.name(alias));
      return aliasResponse.result().keySet();
    } catch (ElasticsearchException e) {
      if (e.response().status() == 404) {
        return Collections.emptySet();
      } else {
        throw e;
      }
    }
  }
}

//    public <T> InputStream searchStream(SearchRequest request, Class<T> docClass) throws
// IOException {
//
//        OpenPointInTimeResponse pitResponse = null;
//        try {
//            pitResponse = esClient.openPointInTime(fn -> fn.index(request.index()).keepAlive(t ->
// t.time("5m")));
//            String pitId = pitResponse.id();
//            PointInTimeReference pitRef = PointInTimeReference.of(p -> p.id(pitId));
//
//            List<SortOptions> sortOptions = request.sort();
//            sortOptions.add(SortOptions.of(s -> s.field(f ->
// f.field("_shard_doc").order(SortOrder.Asc))));
//            SearchRequest.Builder builder = new
// SearchRequest.Builder().index(request.index()).query(request.query())
//                    .size(MAX_RESULT).sort(sortOptions).source(request.source()).pit(pitRef);
//
//            // Pipe Streams are a bit tricky to optimize. If consumer is faster than producer, it
// could be useful, after writing some data,
//            // to flush the outputstream in order to notify the consumer that data is available
// for reading. If producer is faster than consumer,
//            // after reading some data, it could help to call inputstream notify, in order to
// notify the producer that space is available for writing.
//            // for writing.
//            // cf. https://fabsk.eu/blog/2014/05/18/java-pipedinputstream-contention/
//            // cf.
// https://stackoverflow.com/questions/28617175/did-i-find-a-bug-in-java-io-pipedinputstream
//
//            PipedOutputStream pos = new PipedOutputStream();
//            PipedInputStream pis = new PipedInputStream(pos, 4096);
//            Executors.newCachedThreadPool().submit(() -> this.searchAfter(builder, docClass, pos,
// pitId));
//            return pis;
//        }
//        finally {
//            if (pitResponse != null) {
//                String pitId = pitResponse.id();
//                esClient.closePointInTime(fn -> fn.id(pitId));
//            }
//        }
//    }
//
//    private <T> void searchAfter(SearchRequest.Builder builder, Class<T> docClass,
// PipedOutputStream pos, String pitId) {
//
//        try {
//            SearchRequest nextRequest = builder.build();
//            while (true) {
//                SearchResponse<T> response = search(nextRequest, docClass);
//                HitsMetadata<T> hitsMetadata = response.hits();
//                if (hitsMetadata.total().relation() == TotalHitsRelation.Eq) {
//                    break;
//                }
//                List<Hit<T>> hits = hitsMetadata.hits();
//                List<T> objects = hits.stream().map(Hit::source).toList();
//                for (T object : objects) {
//                    JsonService.write(object, pos, JsonConfig.DEFAULT);
//                }
//                pos.flush();
//                Hit<T> hit = hits.get(hits.size() - 1);
//                nextRequest = builder.searchAfter(hit.sort()).build();
//            }
//        }
//        catch (IOException ex) {
//            throw new InternalException(ex);
//        }
//        finally {
//            try {
//                pos.close();
//            }
//            catch (IOException ex) {
//                throw new InternalException(ex);
//            }
//            finally {
//                try {
//                    esClient.closePointInTime(fn -> fn.id(pitId));
//                }
//                catch (IOException ex) {
//                    throw new InternalException(ex);
//                }
//            }
//        }
//    }
//    private <T> void searchAfter(SearchRequest.Builder builder, Class<T> docClass,
// BlockingQueue<T> blockingQueue, String pitId) {
//        try {
//            SearchRequest nextRequest = builder.build();
//            while (true) {
//                SearchResponse<T> response = search(nextRequest, docClass);
//                HitsMetadata<T> hitsMetadata = response.hits();
//                if (hitsMetadata.total().relation() == TotalHitsRelation.Eq) {
//                    break;
//                }
//                List<Hit<T>> hits = hitsMetadata.hits();
//                List<T> objects = hits.stream().map(Hit::source).toList();
//                for (T object : objects) {
//                    blockingQueue.put(object);
//                }
//                Hit<T> hit = hits.get(hits.size() - 1);
//                nextRequest = builder.searchAfter(hit.sort()).build();
//            }
//        }
//        catch (IOException | InterruptedException ex) {
//            throw new InternalException(ex);
//        }
//        finally {
//            try {
//                esClient.closePointInTime(fn -> fn.id(pitId));
//            }
//            catch (IOException ex) {
//                throw new InternalException(ex);
//            }
//        }
//    }
