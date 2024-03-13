/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceConfig.Builder;
import co.elastic.clients.util.ObjectBuilder;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.EqlParser;
import fr.xelians.esafe.search.domain.dsl.parser.eql.RootQuery;
import fr.xelians.esafe.search.domain.field.Field;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;

public abstract class ArchiveUnitParser extends EqlParser {

  protected static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  protected static final String QUERY_IS_EMPTY_OR_NOT_DEFINED = "Query is empty or not defined";
  protected static final String PROJECTION_IS_NOT_DEFINED = "Projection is not defined";
  protected static final String ACCESS_CONTRACT_IS_INACTIVE = "Access Contrat '%s' is inactive";
  protected static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL =
      "Access Contract must be not null";
  protected static final String ONTOLOGY_MAPPER_MUST_BE_NOT_NULL =
      "Ontology Mapper must be not null";

  protected static final String UNIT_ID = "_unitId";
  protected static final String UP = "_up";
  protected static final String US = "_us";
  protected static final String SP = "_sp";
  protected static final String MGT = "_mgt";
  protected static final String UPS_UP = "_ups._up";

  private static final String DOCUMENT_TYPE = "DocumentType";
  private static final String ORIGINATING_AGENCY_IDENTIFIER = "OriginatingAgencyIdentifier";

  private static final String EXT = Field.EXT + ".*";
  private static final String UPS = "_ups.*";

  protected final AccessContractDb accessContractDb;
  protected final OntologyMapper ontologyMapper;

  protected ArchiveUnitParser(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    super(ArchiveUnitIndex.INSTANCE, tenant);
    this.accessContractDb = accessContractDb;
    this.ontologyMapper = ontologyMapper;
  }

  protected SearchRequest doCreateActionSearchRequest(
      SearchContext searchContext, UpdateQuery updateQuery) {

    // Obtain root units
    List<Long> roots =
        updateQuery.roots() == null
            ? Collections.emptyList()
            : updateQuery.roots().stream().filter(id -> id >= 0).toList();

    // Obtain root query
    RootQuery rootQuery = createRootQuery(searchContext, updateQuery.queryNode());
    int depth = Math.max(0, rootQuery.depth());

    // Create filter queries
    List<Query> filterQueries = createFilterQueries(searchContext, roots, depth);

    // Create sort options
    List<SortOptions> sortOptions = createSortOptions(searchContext, updateQuery.filterNode());

    // Create from & size
    int[] limits = createLimits(updateQuery.filterNode());

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

  @Override
  public OntologyMapper getOntologyMapper() {
    return ontologyMapper;
  }

  public AccessContractDb getAccessContract() {
    return accessContractDb;
  }

  protected List<Query> createFilterQueries(
      SearchContext searchContext, List<Long> roots, int depth) {
    List<Query> filterQueries = super.createFilterQueries();

    String docType = searchContext.getDocType();

    // Check document type
    if (StringUtils.isNotBlank(docType)) {
      TermQuery typeQuery =
          TermQuery.of(t -> t.field(DOCUMENT_TYPE).value(v -> v.stringValue(docType)));
      filterQueries.add(typeQuery._toQuery());
    }

    // Check allowed root units
    Set<Long> allowedRoots = accessContractDb.getRootUnits();
    if (!allowedRoots.isEmpty()) {
      filterQueries.add(createAllowedQuery(allowedRoots));
    }

    // Check excluded root units
    Set<Long> excludedRoots = accessContractDb.getExcludedRootUnits();
    if (!excludedRoots.isEmpty()) {
      filterQueries.add(createExcludedQuery(excludedRoots));
    }

    // Check originating agencies
    if (Utils.isFalse(accessContractDb.getEveryOriginatingAgency())) {
      Set<String> oriAgencies = accessContractDb.getOriginatingAgencies();
      if (!oriAgencies.isEmpty()) {
        filterQueries.add(createOriAgenciesQuery(oriAgencies));
      }
    }

    // TODO filter selon, le ruleType

    // Check query roots
    if (roots == null || roots.isEmpty()) {
      if (depth == 0) {
        throw new BadRequestException(
            CREATION_FAILED, "Failed to create query without roots and zero depth");
      } else if (depth == 1) {
        // Filter from absolute root
        Query filter =
            new Query.Builder().term(t -> t.field(UP).value(v -> v.longValue(-1))).build();
        filterQueries.add(filter);
      } else if (depth <= ArchiveUnitIndex.UPS_SIZE) {
        // Filter from absolute root
        Query filter =
            new Query.Builder()
                .term(t -> t.field(UPS_UP + depth).value(v -> v.longValue(-1)))
                .build();
        filterQueries.add(filter);
      }
    } else {
      List<FieldValue> values =
          roots.stream().map(v -> new FieldValue.Builder().longValue(v).build()).toList();
      if (depth == 0) {
        Query filter =
            new Query.Builder().terms(t -> t.field(UNIT_ID).terms(b -> b.value(values))).build();
        filterQueries.add(filter);
      } else if (depth == 1) {
        Query filter =
            new Query.Builder().terms(t -> t.field(UP).terms(b -> b.value(values))).build();
        filterQueries.add(filter);
      } else if (depth <= ArchiveUnitIndex.UPS_SIZE) {
        Query filter =
            new Query.Builder()
                .terms(t -> t.field(UPS_UP + depth).terms(b -> b.value(values)))
                .build();
        filterQueries.add(filter);
      } else {
        Query filter =
            new Query.Builder().terms(t -> t.field(US).terms(b -> b.value(values))).build();
        filterQueries.add(filter);
      }
    }
    return filterQueries;
  }

  private static Query createOriAgenciesQuery(Set<String> oriAgencies) {
    List<FieldValue> values = oriAgencies.stream().map(FieldValue::of).toList();
    TermsQuery q1 =
        TermsQuery.of(t -> t.field(ORIGINATING_AGENCY_IDENTIFIER).terms(v -> v.value(values)));
    return BoolQuery.of(b -> b.should(q1._toQuery()))._toQuery();
  }

  private static Query createAllowedQuery(Set<Long> units) {
    List<FieldValue> values = units.stream().map(FieldValue::of).toList();
    TermsQuery tq1 = TermsQuery.of(t -> t.field(US).terms(v -> v.value(values)));
    TermsQuery tq2 = TermsQuery.of(t -> t.field(UNIT_ID).terms(v -> v.value(values)));
    return BoolQuery.of(b -> b.should(tq1._toQuery(), tq2._toQuery()))._toQuery();
  }

  private static Query createExcludedQuery(Set<Long> units) {
    List<FieldValue> values = units.stream().map(FieldValue::of).toList();
    Query q1 = new Query.Builder().terms(t -> t.field(US).terms(v -> v.value(values))).build();
    Query q2 = new Query.Builder().terms(t -> t.field(UNIT_ID).terms(v -> v.value(values))).build();
    return new Query.Builder().bool(b -> b.mustNot(q1, q2)).build();
  }

  protected Function<Builder, ObjectBuilder<SourceConfig>> createSourceFilter(
      List<String> projectionFields) {
    return s ->
        s.filter(
            projectionFields.isEmpty()
                ? f -> f.excludes(EXT).excludes(UPS)
                : f -> f.includes(projectionFields));
  }
}
