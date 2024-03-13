/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.ExpandWildcard;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.ExistsRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import fr.xelians.esafe.archive.domain.search.search.SearchAfterSpliterator;
import fr.xelians.esafe.archive.domain.search.search.StreamRequest;
import fr.xelians.esafe.common.entity.searchengine.DocumentSe;
import fr.xelians.esafe.search.domain.dsl.bucket.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
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

  public Map<String, List<Bucket>> getFacets(Map<String, Aggregate> aggregations) {
    Map<String, List<Bucket>> bucketsMap = new HashMap<>();
    aggregations.forEach(
        (k, v) -> {
          final List<Bucket> buckets = bucketsMap.computeIfAbsent(k, key -> new ArrayList<>());
          if (v.isSterms()) {
            v.sterms()
                .buckets()
                .array()
                .forEach(
                    bk -> buckets.add(new StringBucket(bk.docCount(), bk.key().stringValue())));
          } else if (v.isLterms()) {
            v.lterms()
                .buckets()
                .array()
                .forEach(bk -> buckets.add(new LongBucket(bk.docCount(), bk.key())));
          } else if (v.isDterms()) {
            v.dterms()
                .buckets()
                .array()
                .forEach(bk -> buckets.add(new DoubleBucket(bk.docCount(), bk.key())));
          } else if (v.isDateRange()) {
            v.dateRange()
                .buckets()
                .array()
                .forEach(
                    bk ->
                        buckets.add(
                            new DateRangeBucket(
                                bk.docCount(), bk.fromAsString(), bk.toAsString(), bk.key())));
          } else if (v.isFilters()) {
            v.filters()
                .buckets()
                .keyed()
                .forEach((n, b) -> buckets.add(new StringBucket(b.docCount(), n)));
          }
        });
    return bucketsMap;
  }

  // Document methods
  public <T extends DocumentSe> void index(String index, T docClass) throws IOException {
    esClient.index(b -> b.index(index).id(String.valueOf(docClass.getId())).document(docClass));
  }

  public <T extends DocumentSe> void bulkIndex(String index, List<T> docsClass) throws IOException {
    List<BulkOperation> bulkList =
        docsClass.stream()
            .map(
                doc ->
                    new BulkOperation.Builder()
                        .index(b -> b.index(index).id(String.valueOf(doc.getId())).document(doc))
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
              "Failed to bulk index documents in index '%s' - Caused by: %s", index, reason));
    }
  }

  public <T extends DocumentSe> void bulkIndexRefresh(String index, List<T> docsClass)
      throws IOException {
    List<BulkOperation> bulkList =
        docsClass.stream()
            .map(
                doc ->
                    new BulkOperation.Builder()
                        .index(b -> b.index(index).id(String.valueOf(doc.getId())).document(doc))
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
              "Failed to bulk index documents in index '%s' - Caused by: %s", index, reason));
    }
  }

  public void bulkDelete(String index, List<Long> ids) throws IOException {
    List<BulkOperation> bulkList =
        ids.stream()
            .map(
                id ->
                    new BulkOperation.Builder()
                        .delete(d -> d.index(index).id(id.toString()))
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
              "Failed to bulk delete documents in index '%s' - Caused by: %s", index, reason));
    }
  }

  public void bulkDeleteRefresh(String index, List<Long> ids) throws IOException {
    List<BulkOperation> bulkList =
        ids.stream()
            .map(
                id ->
                    new BulkOperation.Builder()
                        .delete(d -> d.index(index).id(id.toString()))
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
              "Failed to bulk delete documents in index '%s' - Caused by: %s", index, reason));
    }
  }

  // Index methods
  public void createIndex(String index, String alias, String mapping) throws IOException {
    log.info("Create index {} with alias {} ", index, alias);

    BooleanResponse indexExists =
        esClient.indices().exists(existsBuilder -> existsBuilder.index(index));
    if (indexExists.value()) {
      throw new IOException(String.format("Failed to create already existent index '%s'", index));
    }

    indexExists = esClient.indices().exists(e -> e.index(alias));
    if (indexExists.value()) {
      log.info("CreateIndex() - Index {} exists", alias);
      DeleteIndexResponse deleteIndex = esClient.indices().delete(d -> d.index(alias));
      if (!deleteIndex.acknowledged()) {
        throw new IOException(String.format("Failed to delete index %s", alias));
      }
      log.info("CreateIndex() - Index {} deleted", alias);

      //      throw new IOException(String.format("Failed to create already existent index '%s'",
      // alias));
    }

    BooleanResponse aliasExists = esClient.indices().existsAlias(e -> e.name(alias));
    if (aliasExists.value()) {
      throw new IOException(
          String.format(
              "Failed to create index '%s' with already existent alias '%s'", index, alias));
    }

    CreateIndexResponse createIndex =
        esClient
            .indices()
            .create(
                createBuilder ->
                    createBuilder
                        .index(index)
                        .aliases(alias, ab -> ab.isWriteIndex(true))
                        .mappings(builder -> builder.withJson(new StringReader(mapping))));
    // .settings(s -> s.numberOfShards("2").numberOfReplicas("2"))

    if (!createIndex.acknowledged()) {
      throw new IOException(String.format("Failed to create index '%s'", index));
    }

    log.info("Create index {} done", createIndex.index());
  }

  public void deleteIndexWithPrefix(String index) throws IOException {
    //  TODO be sure to delete only user index
    //  GetIndexResponse getIndex = esClient.indices().get(g -> g.index("_all"));
    GetIndexResponse getIndex =
        esClient.indices().get(g -> g.index(index + "*").expandWildcards(ExpandWildcard.All));

    List<String> indexList = new ArrayList<>(getIndex.result().keySet());
    if (!indexList.isEmpty()) {
      DeleteIndexResponse deleteIndex = esClient.indices().delete(d -> d.index(indexList));
      if (!deleteIndex.acknowledged()) {
        throw new IOException(String.format("Failed to delete all index %s", indexList));
      }
      log.info("deleteIndexWithPrefix() - Index {} - {} deleted", index, indexList);
    } else {
      log.info("deleteIndexWithPrefix() - Index {} is empty", index);
    }
  }

  public void deleteIndex(String index) throws IOException {
    BooleanResponse indexExists = esClient.indices().exists(e -> e.index(index));

    if (indexExists.value()) {
      DeleteIndexResponse deleteIndex = esClient.indices().delete(d -> d.index(index));
      if (!deleteIndex.acknowledged()) {
        throw new IOException(String.format("Failed to delete index %s", index));
      }
      log.info("deleteIndex() - Index {} deleted", index);
    } else {
      log.info("deleteIndex() - Index {} does not exist", index);
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
      // Do not delete index (wait a few day before!)
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

  public void moveAliasTo(String alias, String indexName) throws IOException {
    log.info("moveAliasTo() - Alias: {} - indexName: {}", alias, indexName);

    removeAlias(alias);

    // Add alias to new Index
    PutAliasResponse putAliasResponse =
        esClient.indices().putAlias(p -> p.index(indexName).name(alias).isWriteIndex(true));
    if (!putAliasResponse.acknowledged()) {
      throw new IOException(
          String.format("Failed to assign alias '%s' to index '%s'", alias, indexName));
    }

    log.info("moveAliasTo() - add Alias {} to Index {} done", alias, indexName);
  }

  public void refresh(String index) throws IOException {
    esClient.indices().refresh(r -> r.index(index));
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
