/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitParser;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.RootQuery;
import io.jsonwebtoken.lang.Assert;
import java.util.Collections;
import java.util.List;

public class ProbativeValueParser extends ArchiveUnitParser {

  public ProbativeValueParser(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    super(tenant, accessContractDb, ontologyMapper);
  }

  public static ProbativeValueParser create(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContractDb, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(ontologyMapper, ONTOLOGY_MAPPER_MUST_BE_NOT_NULL);

    return new ProbativeValueParser(tenant, accessContractDb, ontologyMapper);
  }

  public SearchRequest createRequest(SearchQuery searchQuery) {

    if (isEmpty(searchQuery.queryNode())) {
      throw new BadRequestException(CREATION_FAILED, QUERY_IS_EMPTY_OR_NOT_DEFINED);
    }

    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format(ACCESS_CONTRACT_IS_INACTIVE, accessContractDb.getIdentifier()));
    }

    // The context of this query
    SearchContext searchContext = new SearchContext(searchQuery.type());

    // Obtain root units
    List<Long> roots =
        searchQuery.roots() == null
            ? Collections.emptyList()
            : searchQuery.roots().stream().filter(id -> id >= 0).toList();

    // Obtain root query
    RootQuery rootQuery = createRootQuery(searchContext, searchQuery.queryNode());
    int depth = Math.max(0, rootQuery.depth());

    // Create filter queries
    List<Query> filterQueries = createFilterQueries(searchContext, roots, depth);

    // Create sort options
    List<SortOptions> sortOptions = createSortOptions(searchContext, searchQuery.filterNode());

    // Create from & size
    int[] limits = createLimits(searchQuery.filterNode());

    // Create search request
    return SearchRequest.of(
        s ->
            s.index(searchable.getAlias())
                .query(b -> b.bool(m -> m.must(rootQuery.query()).filter(filterQueries)))
                .from(limits[FROM])
                .size(limits[SIZE])
                .sort(sortOptions)
                .source(createSourceFilter(Collections.emptyList())));
  }
}
