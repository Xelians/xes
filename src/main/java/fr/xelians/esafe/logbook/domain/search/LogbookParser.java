/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.logbook.domain.search;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceConfig.Builder;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.EqlParser;
import fr.xelians.esafe.search.domain.dsl.parser.eql.RootQuery;
import io.jsonwebtoken.lang.Assert;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LogbookParser extends EqlParser {

  private static final JsonNode QUERY_ALL = queryAll();

  private LogbookParser(Long tenant) {
    super(LogbookIndex.INSTANCE, tenant);
  }

  public static LogbookParser create(Long tenant) {
    Assert.notNull(tenant, "Tenant must be not null");
    return new LogbookParser(tenant);
  }

  @Override
  public OntologyMapper getOntologyMapper() {
    return null;
  }

  public SearchRequest createSearchRequest(SearchQuery searchQuery) {

    if (searchQuery.projectionNode() == null) {
      throw new BadRequestException(CREATION_FAILED, "Projection is not defined");
    }
    if (searchQuery.type() != null) {
      throw new BadRequestException(CREATION_FAILED, "This query dos not support $type");
    }
    if (searchQuery.roots() != null) {
      throw new BadRequestException(CREATION_FAILED, "This query dos not support $roots");
    }

    // Create the context of this query
    SearchContext searchContext = new SearchContext();

    // Obtain root query
    JsonNode queryNode = isEmpty(searchQuery.queryNode()) ? QUERY_ALL : searchQuery.queryNode();
    RootQuery rootQuery = createRootQuery(searchContext, queryNode);

    // Create filter queries
    List<Query> filterQueries = createFilterQueries();

    // Create sort options
    List<SortOptions> sortOptions = createSortOptions(searchContext, searchQuery.filterNode());

    // Create from & size
    int[] limits = createLimits(searchQuery.filterNode());

    // Create projection
    List<String> projectionFields =
        createProjectionFields(searchContext, searchQuery.projectionNode());

    // Create facets
    Map<String, Aggregation> aggregations =
        createAggregations(searchContext, searchQuery.facetsNode());

    // Create search request
    return SearchRequest.of(
        s ->
            s.index(searchable.getAlias())
                .query(b -> b.bool(m -> m.must(rootQuery.query()).filter(filterQueries)))
                .from(limits[FROM])
                .size(limits[SIZE])
                .aggregations(aggregations)
                .sort(sortOptions)
                .source(createSourceFilter(projectionFields)));
  }

  private Function<Builder, ObjectBuilder<SourceConfig>> createSourceFilter(
      List<String> projectionFields) {
    return s -> s.filter(f -> f.includes(projectionFields));
  }

  private static JsonNode queryAll() {
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();
    queryNode.put("$exists", "#id");
    return queryNode;
  }
}
