/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.dto.TenantContract;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.domain.search.ReferentialParser;
import fr.xelians.esafe.referential.domain.search.Request;
import fr.xelians.esafe.referential.dto.ReferentialDto;
import fr.xelians.esafe.referential.entity.ReferentialDb;
import fr.xelians.esafe.referential.repository.IRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

public abstract class AbstractReferentialService<
    D extends ReferentialDto, E extends ReferentialDb> {

  public static final String IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY =
      "identifier must be not null nor empty";
  public static final String ENTITY_NOT_FOUND = "Entity not found";
  public static final String TENANT_MUST_BE_NOT_NULL = "tenant must be not null";

  protected final EntityManager entityManager;
  protected final IRepository<E> repository;
  protected final OperationService operationService;

  // @PersistentContext
  protected AbstractReferentialService(
      EntityManager entityManager, IRepository<E> repository, OperationService operationService) {
    this.entityManager = entityManager;
    this.repository = repository;
    this.operationService = operationService;
  }

  @SuppressWarnings("unchecked")
  protected String getEntityName() {
    return ((Class<D>)
            ((ParameterizedType) this.getClass().getGenericSuperclass())
                .getActualTypeArguments()[0])
        .getSimpleName()
        .replace("Dto", "");
  }

  @SuppressWarnings("unchecked")
  protected D createDto() {
    try {
      return ((Class<D>)
              ((ParameterizedType) this.getClass().getGenericSuperclass())
                  .getActualTypeArguments()[0])
          .getDeclaredConstructor()
          .newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException ex) {
      throw new InternalException("Dto creation failed", "Failed to create dto", ex);
    }
  }

  @SuppressWarnings("unchecked")
  protected E createEntity() {
    try {
      return ((Class<E>)
              ((ParameterizedType) this.getClass().getGenericSuperclass())
                  .getActualTypeArguments()[1])
          .getDeclaredConstructor()
          .newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | NoSuchMethodException
        | InvocationTargetException ex) {
      throw new InternalException("Entity creation failed", "Failed to create entity", ex);
    }
  }

  protected D toDto(E entity) {
    return Utils.copyProperties(entity, createDto());
  }

  protected E toEntity(D dto) {
    return Utils.copyProperties(dto, createEntity());
  }

  protected OperationType createOperationType() {
    return OperationType.valueOf("CREATE_" + getEntityName().toUpperCase());
  }

  protected OperationType updateOperationType() {
    return OperationType.valueOf("UPDATE_" + getEntityName().toUpperCase());
  }

  // Ideally, we should run "create" in a full write table lock transaction.
  // But, even in Serializable mode, most databases try to concurrently run all transactions
  // and fail (rollback) if they detect read/write dependencies during transactions.
  // In our case, if we get an anomaly, we will always fail because of the composite
  // (tenant + identifier) index. So we don't need to support this transaction cost.
  @Transactional
  public List<D> create(Long tenant, String userIdentifier, String applicationId, List<D> dtos) {
    Assert.notNull(dtos, String.format("%s dto cannot be null", getEntityName()));

    String prefix = getEntityName().toUpperCase() + "-";

    OperationDb operation = createOperation(tenant, userIdentifier, applicationId);
    List<Long> entityIds = new ArrayList<>(dtos.size());

    long next = 1;
    List<D> saveDtos = new ArrayList<>(dtos.size());
    for (D dto : dtos) {
      if (dto.getTenant() != null && !tenant.equals(dto.getTenant())) {
        throw new BadRequestException(
            "Entity creation failed",
            String.format(
                "Entity %s tenant mismatch: %s vs %s", getEntityName(), tenant, dto.getTenant()));
      }

      if (dto.getIdentifier() == null) {
        next = next == 1 ? nextIdentifier(tenant, prefix) : next + 1;
        dto.setIdentifier(Utils.padIdentifier(prefix, next));
      }

      E entity = toEntity(dto);
      entity.setTenant(tenant);
      entity.setOperationId(operation.getId());
      entity.setAutoVersion(1);
      entity.setLfcs(null);
      entity.setCreationDate(operation.getCreated().toLocalDate());
      entity.setLastUpdate(entity.getCreationDate());
      E savedEntity = repository.save(entity);

      saveDtos.add(toDto(savedEntity));
      entityIds.add(savedEntity.getId());
    }

    operation.setProperty01(StringUtils.join(entityIds, ','));
    return saveDtos;
  }

  // Find the next available identifier in the database. The identifier is not "reserved",
  // so we can get an already used identifier in case of multiple simultaneous calls.
  // This can yield a transaction rollback because the identifier must be unique.
  private long nextIdentifier(Long tenant, String prefix) {
    List<String> identifiers = repository.findIdentifiersByTenantAndStartingWith(tenant, prefix);
    return identifiers.stream()
        .map(identifier -> identifier.substring(prefix.length()))
        .filter(StringUtils::isNumeric)
        .mapToLong(Long::parseLong)
        .map(n -> n + 1)
        .max()
        .orElse(1);
  }

  @Transactional
  public D update(
      Long tenant, String userIdentifier, String applicationId, String identifier, D dto) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);
    Assert.notNull(dto, "dto cannot be null");

    if (dto.getIdentifier() != null && !identifier.equals(dto.getIdentifier())) {
      throw new BadRequestException(
          "Entity update failed",
          String.format(
              "Entity %s identifiers mismatch: %s vs %s",
              getEntityName(), identifier, dto.getIdentifier()));
    }

    if (dto.getTenant() != null && !tenant.equals(dto.getTenant())) {
      throw new BadRequestException(
          "Entity update failed",
          String.format(
              "Entity %s tenant mismatch: %s vs %s", getEntityName(), tenant, dto.getTenant()));
    }

    // Get entity from db
    E entity = getEntity(tenant, identifier);
    E oriEntity = copyDtoToEntity(dto, entity);

    // Create operation
    OperationDb operation = updateOperation(tenant, userIdentifier, applicationId);
    operation.setProperty01(entity.getId().toString());

    // Add LifeCycle
    JsonNode dtoNode = JsonService.toJson(toDto(entity));
    JsonNode oriDtoNode = JsonService.toJson(toDto(oriEntity));
    JsonNode patchNode = JsonDiff.asJson(dtoNode, oriDtoNode);
    String patch = JsonService.toString(patchNode);

    entity.addLifeCycle(
        new LifeCycle(
            entity.getAutoVersion(),
            operation.getId(),
            operation.getType(),
            operation.getCreated(),
            patch));
    entity.incAutoVersion();
    entity.setLastUpdate(operation.getCreated().toLocalDate());
    return toDto(entity);
  }

  // Override this method when updating entity
  public E copyDtoToEntity(D dto, E entity) {
    // Keep off non-updatable fields
    E oriEntity = Utils.copyProperties(entity, createEntity());
    Utils.copyProperties(dto, entity);
    entity.setCreationDate(oriEntity.getCreationDate());
    entity.setLastUpdate(oriEntity.getLastUpdate());
    entity.setOperationId(oriEntity.getOperationId());
    entity.setAutoVersion(oriEntity.getAutoVersion());
    entity.setLfcs(oriEntity.getLfcs());
    entity.setTenant(oriEntity.getTenant());
    return oriEntity;
  }

  private OperationDb createOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op =
        OperationFactory.createReferentialOp(
            createOperationType(), tenant, userIdentifier, applicationId);

    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Create referential");
    return operationService.save(op);
  }

  private OperationDb updateOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op =
        OperationFactory.updateReferentialOp(
            updateOperationType(), tenant, userIdentifier, applicationId);

    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Update referential");
    return operationService.save(op);
  }

  public void delete(String identifier, final Long tenant) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);

    Long id =
        repository
            .findIdByTenantAndIdentifier(tenant, identifier)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        ENTITY_NOT_FOUND,
                        String.format(
                            "Entity %s with id %s nod found", getEntityName(), identifier)));

    repository.deleteById(id);
  }

  public D getDto(Long tenant, String identifier) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);

    return repository
        .findByTenantAndIdentifier(tenant, identifier)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Dto not found",
                    String.format(
                        "'%s' with identifier '%s' not found", getEntityName(), identifier)));
  }

  public void checkTenantContractExists(Set<Long> tenants, List<TenantContract> tenantContracts) {
    for (TenantContract tc : tenantContracts) {
      Long tenant = tc.getTenant();

      if (!tenants.contains(tenant)
          || !repository.existsByTenantAndIdentifier(tenant, tc.getIdentifier())) {
        throw new NotFoundException(
            "Failed to check tenant contracts",
            String.format(
                "Cannot found contract with tenant '%s' and identifier '%s'",
                tenant, tc.getIdentifier()));
      }
    }
  }

  public List<D> getDtos() {
    Long tenant = AuthContext.getTenant();
    return repository.findByTenant(tenant).stream().map(this::toDto).toList();
  }

  public List<D> getDtosByName(String name) {
    Assert.hasText(name, "name cannot be null or empty");

    Long tenant = AuthContext.getTenant();
    return repository.findByTenantAndName(tenant, name).stream().map(this::toDto).toList();
  }

  public PageResult<D> getDtos(Long tenant, String name, Status status, Pageable pageable) {
    Page<E> page;
    if (name != null && status != null) {
      page = repository.findByTenantAndNameAndStatus(tenant, name, status, pageable);
    } else if (name != null) {
      page = repository.findByTenantAndName(tenant, name, pageable);
    } else if (status != null) {
      page = repository.findByTenantAndStatus(tenant, status, pageable);
    } else {
      page = repository.findByTenant(tenant, pageable);
    }
    return new PageResult<>(page.map(this::toDto));
  }

  // Internal use
  public List<D> getDtos(Long tenant) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return repository.findByTenant(tenant).stream().map(this::toDto).toList();
  }

  public List<E> getEntities(Long tenant) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return repository.findByTenant(tenant);
  }

  public E getEntity(Long tenant, String identifier) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return repository
        .findByTenantAndIdentifier(tenant, identifier)
        .orElseThrow(
            () ->
                new NotFoundException(
                    ENTITY_NOT_FOUND,
                    String.format(
                        "'%s' with identifier '%s' not found", getEntityName(), identifier)));
  }

  public Optional<E> getOptionalEntity(Long tenant, String identifier) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    return repository.findByTenantAndIdentifier(tenant, identifier);
  }

  public boolean existsByIdentifier(String identifier) {
    Assert.hasText(identifier, IDENTIFIER_MUST_BE_NOT_NULL_NOR_EMPTY);
    Long tenant = AuthContext.getTenant();
    return repository.existsByTenantAndIdentifier(tenant, identifier);
  }

  protected SearchResult<JsonNode> search(ReferentialParser<E> parser, SearchQuery query) {

    List<String> projections = parser.getProjections(query);

    if (projections.isEmpty()) {
      Request<E> request = parser.createRequest(query);
      SearchResult<E> result = repository.search(request, query);
      List<JsonNode> nodes =
          result.results().stream().map(this::toDto).map(JsonService::toJson).toList();
      return new SearchResult<>(
          result.httpCode(), result.hits(), nodes, result.facets(), result.context());
    }

    Request<Object[]> request = parser.createRequest(query, projections);
    SearchResult<JsonNode> result = repository.search(request, query, projections);
    return new SearchResult<>(
        result.httpCode(), result.hits(), result.results(), result.facets(), result.context());
  }
}
