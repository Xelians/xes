/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.domain.search;

import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.referential.entity.*;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import java.util.List;
import org.springframework.util.Assert;

public class ReferentialParser<T> extends SqlParser<T> {

  private ReferentialParser(
      ReferentialIndex referentialIndex,
      Long tenant,
      Class<T> entityClass,
      EntityManager entityManager) {
    super(referentialIndex, tenant, entityClass, entityManager);
  }

  public static ReferentialParser<OntologyDb> createOntologyParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(
        ReferentialIndex.INSTANCE, tenant, OntologyDb.class, entityManager);
  }

  public static ReferentialParser<ProfileDb> createProfileParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(
        ReferentialIndex.INSTANCE, tenant, ProfileDb.class, entityManager);
  }

  public static ReferentialParser<AgencyDb> createAgencyParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(
        ReferentialIndex.INSTANCE, tenant, AgencyDb.class, entityManager);
  }

  public static ReferentialParser<AccessContractDb> createAccessContractParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(
        ReferentialIndex.INSTANCE, tenant, AccessContractDb.class, entityManager);
  }

  public static ReferentialParser<IngestContractDb> createIngestContractParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(
        ReferentialIndex.INSTANCE, tenant, IngestContractDb.class, entityManager);
  }

  public static ReferentialParser<RuleDb> createRuleParser(
      Long tenant, EntityManager entityManager) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return new ReferentialParser<>(ReferentialIndex.INSTANCE, tenant, RuleDb.class, entityManager);
  }

  public List<String> getProjections(SearchQuery searchQuery) {
    if (searchQuery.projectionNode() == null) {
      throw new BadRequestException(CREATION_FAILED, PROJECTION_IS_NOT_DEFINED);
    }

    return createProjectionFields(new SearchContext(), searchQuery.projectionNode());
  }

  // Build query dsl from string
  public Request<T> createRequest(SearchQuery searchQuery) {
    checkSearchQuery(searchQuery);
    SearchContext searchContext = new SearchContext();
    TypedQuery<T> mainQuery = createMainQuery(searchContext, searchQuery);
    TypedQuery<Long> countQuery = createCountQuery(searchContext, searchQuery);
    return new Request<>(mainQuery, countQuery);
  }

  public Request<Object[]> createRequest(SearchQuery searchQuery, List<String> projections) {
    Assert.notEmpty(projections, PROJECTION_IS_NOT_DEFINED);

    checkSearchQuery(searchQuery);
    SearchContext searchContext = new SearchContext();
    TypedQuery<Object[]> mainQuery =
        createMainQueryWithProjection(searchContext, searchQuery, projections);
    TypedQuery<Long> countQuery = createCountQuery(searchContext, searchQuery);
    return new Request<>(mainQuery, countQuery);
  }

  private void checkSearchQuery(SearchQuery searchQuery) {
    Assert.notNull(searchQuery, "Search query must ne not null");

    if (isEmpty(searchQuery.queryNode())) {
      throw new BadRequestException(CREATION_FAILED, QUERY_IS_EMPTY_OR_NOT_DEFINED);
    }

    if (searchQuery.type() != null) {
      throw new BadRequestException(CREATION_FAILED, "This query dos not support $type");
    }

    if (searchQuery.roots() != null) {
      throw new BadRequestException(CREATION_FAILED, "This query dos not support $roots");
    }
  }

  private TypedQuery<T> createMainQuery(SearchContext searchContext, SearchQuery searchQuery) {
    CriteriaQuery<T> mainCriteria = criteriaBuilder.createQuery(entityClass);
    rootEntity = mainCriteria.from(entityClass);
    List<Order> sortOrders = createSortOrders(searchContext, searchQuery.filterNode());
    mainCriteria
        .select(rootEntity)
        .where(wherePredicate(searchContext, searchQuery.queryNode()))
        .orderBy(sortOrders);
    int[] limits = createLimits(searchQuery.filterNode());
    return entityManager
        .createQuery(mainCriteria)
        .setMaxResults(limits[SIZE])
        .setFirstResult(limits[FROM]);
  }

  @SuppressWarnings("unchecked")
  private TypedQuery<Object[]> createMainQueryWithProjection(
      SearchContext searchContext, SearchQuery searchQuery, List<String> projections) {

    CriteriaQuery<Object[]> mainCriteria = criteriaBuilder.createQuery(Object[].class);
    rootEntity = mainCriteria.from(entityClass);
    Path<Object>[] paths = projections.stream().map(p -> rootEntity.get(p)).toArray(Path[]::new);
    List<Order> sortOrders = createSortOrders(searchContext, searchQuery.filterNode());
    mainCriteria
        .multiselect(paths)
        .where(wherePredicate(searchContext, searchQuery.queryNode()))
        .orderBy(sortOrders);
    int[] limits = createLimits(searchQuery.filterNode());
    return entityManager
        .createQuery(mainCriteria)
        .setMaxResults(limits[SIZE])
        .setFirstResult(limits[FROM]);
  }

  private TypedQuery<Long> createCountQuery(SearchContext searchContext, SearchQuery searchQuery) {
    CriteriaQuery<Long> countCriteria = criteriaBuilder.createQuery(Long.class);
    rootEntity = countCriteria.from(entityClass);
    countCriteria
        .select(criteriaBuilder.count(rootEntity))
        .where(wherePredicate(searchContext, searchQuery.queryNode()));
    return entityManager.createQuery(countCriteria);
  }
}
