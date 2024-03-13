/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationQuery;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.entity.OperationDb;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.query.QueryUtils;

@Slf4j
@RequiredArgsConstructor
public class CustomOperationRepositoryImpl implements CustomOperationRepository {

  // @PersistentContext
  private final EntityManager entityManager;

  @Override
  public Slice<OperationStatusDto> findOperationStatus(
      Long tenant, OperationQuery operationQuery, PageRequest pageRequest) {

    List<OperationStatusDto> results =
        getOperations(tenant, operationQuery, pageRequest).stream()
            .map(
                entity ->
                    new OperationStatusDto(entity.getId(), entity.getStatus(), entity.getMessage()))
            .toList();
    return generateResult(results, pageRequest);
  }

  @Override
  public Slice<OperationDto> searchOperationDtos(
      Long tenant, OperationQuery operationQuery, PageRequest pageRequest) {
    List<OperationDto> results =
        getOperations(tenant, operationQuery, pageRequest).stream()
            .map(
                entity ->
                    new OperationDto(
                        entity.getId(),
                        entity.getTenant(),
                        entity.getType(),
                        entity.getStatus(),
                        entity.getMessage(),
                        entity.getEvents(),
                        entity.getUserIdentifier(),
                        entity.getApplicationId(),
                        entity.getCreated(),
                        entity.getModified()))
            .toList();
    return generateResult(results, pageRequest);
  }

  private <T> Slice<T> generateResult(List<T> results, PageRequest pageRequest) {
    // We ask an additional element in the request (getPageSize() + 1) to determine if there are
    // more results after this page.
    boolean hasNext = pageRequest.isPaged() && results.size() > pageRequest.getPageSize();
    return new SliceImpl<>(
        hasNext ? results.subList(0, pageRequest.getPageSize()) : results, pageRequest, hasNext);
  }

  private List<OperationDb> getOperations(
      Long tenant, OperationQuery operationQuery, PageRequest pageRequest) {

    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<OperationDb> criteriaQuery = criteriaBuilder.createQuery(OperationDb.class);
    Root<OperationDb> rootEntity = criteriaQuery.from(OperationDb.class);

    criteriaQuery
        .select(rootEntity)
        .where(generatePredicates(tenant, operationQuery, rootEntity, criteriaBuilder))
        .orderBy(QueryUtils.toOrders(pageRequest.getSort(), rootEntity, criteriaBuilder));

    return entityManager
        .createQuery(criteriaQuery)
        // We have an additional element to determine if there are more results after this page.
        .setMaxResults(pageRequest.getPageSize() + 1)
        .setFirstResult(pageRequest.getPageNumber() * pageRequest.getPageSize())
        .getResultList();
  }

  private Predicate[] generatePredicates(
      Long tenant,
      OperationQuery operationQuery,
      Root<OperationDb> rootEntity,
      CriteriaBuilder criteriaBuilder) {

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(criteriaBuilder.equal(rootEntity.get("tenant"), tenant));

    if (StringUtils.isNotBlank(operationQuery.id())) {
      predicates.add(criteriaBuilder.equal(rootEntity.get("id"), operationQuery.id()));
    }

    // States does not exist in operationDb but status is derived from state
    if (CollectionUtils.isNotEmpty(operationQuery.states())) {
      predicates.add(rootEntity.get("status").in(statusFromStates(operationQuery.states())));
    }

    if (CollectionUtils.isNotEmpty(operationQuery.statuses())) {
      predicates.add(rootEntity.get("status").in(statusFromString(operationQuery.statuses())));
    }

    if (CollectionUtils.isNotEmpty(operationQuery.types())) {
      predicates.add(rootEntity.get("type").in(operationQuery.types()));
    }

    if (operationQuery.startDateMin() != null) {
      predicates.add(
          criteriaBuilder.greaterThanOrEqualTo(
              rootEntity.get("created"), operationQuery.startDateMin()));
    }

    if (operationQuery.startDateMax() != null) {
      predicates.add(
          criteriaBuilder.lessThanOrEqualTo(
              rootEntity.get("created"), operationQuery.startDateMax()));
    }

    return predicates.toArray(new Predicate[0]);
  }

  private static List<OperationStatus> statusFromString(Set<String> strings) {
    return strings.stream().map(OperationStatus::valueOf).toList();
  }

  private static List<OperationStatus> statusFromStates(Set<OperationState> states) {
    List<OperationStatus> list = new ArrayList<>();
    for (OperationStatus opStatus : OperationStatus.values()) {
      for (OperationState state : states) {
        if (opStatus.getState() == state) {
          list.add(opStatus);
          break;
        }
      }
    }
    return list;
  }
}
