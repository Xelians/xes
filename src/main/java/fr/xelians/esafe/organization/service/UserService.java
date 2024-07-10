/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */
/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.flipkart.zjsonpatch.JsonDiff;
import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.domain.role.GlobalRole;
import fr.xelians.esafe.organization.domain.role.TenantRole;
import fr.xelians.esafe.organization.dto.TenantContract;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.organization.repository.UserRepository;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.referential.service.IngestContractService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  public static final String ENTITY_NAME = "User";

  public static final String USER_NOT_FOUND = "User not found";
  public static final String TENANT_MUST_BE_NOT_NULL = "tenant must be not null";
  public static final String USER_IDENTIFIER_MUST_BE_NOT_NULL =
      "userIdentifier must be not null nor empty";
  public static final String ORGANIZATION_ID_MUST_BE_NOT_NULL = "organizationId must be not null";

  private final UserRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final OrganizationService organizationService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final AccessContractService accessContractService;
  private final IngestContractService ingestContractService;
  private final ApiKeyService apiKeyService;

  public UserDto toDto(UserDb userDb) {
    UserDto userDto = Utils.copyProperties(userDb, new UserDto());
    userDto.setPassword(null);

    userDto.setGlobalRoles(new ArrayList<>(userDb.getGlobalRoles()));

    userDto.setTenantRoles(
        userDb.getTenantRoles().stream()
            .map(TenantRole::new)
            .collect(Collectors.toCollection(ArrayList::new)));

    userDto.setAccessContracts(
        userDb.getAccessContracts().stream()
            .map(TenantContract::new)
            .collect(Collectors.toCollection(ArrayList::new)));

    userDto.setIngestContracts(
        userDb.getIngestContracts().stream()
            .map(TenantContract::new)
            .collect(Collectors.toCollection(ArrayList::new)));
    userDto.setOrganizationIdentifier(userDb.getOrganization().getIdentifier());

    return userDto;
  }

  public UserDb toEntity(UserDto userDto) {
    UserDb userDb = Utils.copyProperties(userDto, new UserDb());

    userDb.setGlobalRoles(new HashSet<>(userDto.getGlobalRoles()));

    userDb.setTenantRoles(
        userDto.getTenantRoles().stream()
            .map(TenantRole::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    userDb.setAccessContracts(
        userDto.getAccessContracts().stream()
            .map(TenantContract::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    userDb.setIngestContracts(
        userDto.getIngestContracts().stream()
            .map(TenantContract::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    if (userDto.getPassword() != null) {
      // It obviously takes time to encode the password
      userDb.setPassword(passwordEncoder.encode(userDto.getPassword()));
    }

    return userDb;
  }

  public UserDb copyDtoToEntity(UserDto userDto, UserDb userDb) {
    // Keep off non-updatable fields
    UserDb oriUserDb = Utils.copyProperties(userDb, new UserDb());
    Utils.copyProperties(userDto, userDb);
    userDb.setCreationDate(oriUserDb.getCreationDate());
    userDb.setLastUpdate(oriUserDb.getLastUpdate());
    userDb.setOperationId(oriUserDb.getOperationId());
    userDb.setAutoVersion(oriUserDb.getAutoVersion());
    userDb.setLfcs(oriUserDb.getLfcs());

    userDb.setGlobalRoles(new HashSet<>(userDto.getGlobalRoles()));

    userDb.setTenantRoles(
        userDto.getTenantRoles().stream()
            .map(TenantRole::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    userDb.setAccessContracts(
        userDto.getAccessContracts().stream()
            .map(TenantContract::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    userDb.setIngestContracts(
        userDto.getIngestContracts().stream()
            .map(TenantContract::toString)
            .collect(Collectors.toCollection(HashSet::new)));

    if (userDto.getPassword() != null) {
      // It obviously takes time to encode the password
      userDb.setPassword(passwordEncoder.encode(userDto.getPassword()));
    }
    return oriUserDb;
  }

  // Internal use only
  // Create User
  @Transactional
  public UserDb createEntity(UserDto userDto, OrganizationDb organizationDb, TenantDb tenantDb) {
    Assert.notNull(userDto, String.format("%s dto cannot be null", ENTITY_NAME));

    UserDb userDb = toEntity(userDto);
    userDb.setOperationId(-1L); // Fake value to avoid null constraint
    userDb.setCreationDate(LocalDate.now());
    userDb.setLastUpdate(userDb.getCreationDate());
    userDb.setAutoVersion(1);
    // TODO
    // This is unsafe, implements real feature for set an apikey for a user
    userDb.getApiKey().add(apiKeyService.buildApiKey(tenantDb.getId().toString()));
    userDb.getGlobalRoles().add(GlobalRole.ROLE_ADMIN);
    userDb.setOrganization(organizationDb);
    return repository.save(userDb);
  }

  @Transactional
  public List<UserDto> create(
      Long organizationId, String userIdentifier, String applicationId, List<UserDto> userDtos) {

    Assert.notNull(organizationId, ORGANIZATION_ID_MUST_BE_NOT_NULL);
    Assert.hasText(userIdentifier, USER_IDENTIFIER_MUST_BE_NOT_NULL);
    Assert.notNull(userDtos, String.format("%s userDtos must be not null", ENTITY_NAME));

    // Get all tenants from organization
    Set<Long> tenants =
        tenantService.getTenantDbs(organizationId).stream()
            .map(TenantDb::getId)
            .collect(Collectors.toCollection(HashSet::new));

    // Check all tenant roles
    List<TenantRole> trs = userDtos.stream().flatMap(e -> e.getTenantRoles().stream()).toList();
    for (TenantRole tr : trs) {
      if (!tenants.contains(tr.getTenant())) {
        throw new NotFoundException(
            "Failed to check tenant role", String.format("Bad tenant for tenant role '%s'", tr));
      }
    }

    // Check all access contracts exist
    List<TenantContract> acs =
        userDtos.stream().flatMap(e -> e.getAccessContracts().stream()).toList();
    accessContractService.checkTenantContractExists(tenants, acs);

    // Check all ingest contracts exist
    List<TenantContract> ics =
        userDtos.stream().flatMap(e -> e.getIngestContracts().stream()).toList();
    ingestContractService.checkTenantContractExists(tenants, ics);

    OrganizationDb organizationDb = organizationService.getOrganizationDbById(organizationId);
    Long tenant = organizationDb.getTenant();
    OperationDb operation = createOperation(tenant, userIdentifier, applicationId);
    List<Long> userDbIds = new ArrayList<>(userDtos.size());

    List<UserDto> saveUserDtos = new ArrayList<>(userDtos.size());
    for (UserDto userDto : userDtos) {
      UserDb userDb = toEntity(userDto);
      userDb.setOrganization(organizationDb);
      userDb.setCreationDate(operation.getCreated().toLocalDate());
      userDb.setLastUpdate(userDb.getCreationDate());
      userDb.setOperationId(operation.getId());
      userDb.setAutoVersion(1);
      userDb.setLfcs(null);

      UserDb savedUserDb = repository.save(userDb);
      saveUserDtos.add(toDto(savedUserDb));
      userDbIds.add(savedUserDb.getId());
    }

    operation.setProperty01(StringUtils.join(userDbIds, ','));
    return saveUserDtos;
  }

  @Transactional
  public UserDto update(
      Long organizationId, String userIdentifier, String applicationId, UserDto userDto) {

    Assert.notNull(organizationId, ORGANIZATION_ID_MUST_BE_NOT_NULL);
    Assert.hasText(userIdentifier, USER_IDENTIFIER_MUST_BE_NOT_NULL);
    Assert.notNull(userDto, "userDto must be not null");

    if (userDto.getIdentifier() != null && !userIdentifier.equals(userDto.getIdentifier())) {
      throw new BadRequestException(
          "Failed to update user",
          String.format(
              "%s identifiers mismatch: %s vs %s",
              ENTITY_NAME, userIdentifier, userDto.getIdentifier()));
    }

    // Get all tenants from organization
    Set<Long> tenants =
        tenantService.getTenantDbs(organizationId).stream()
            .map(TenantDb::getId)
            .collect(Collectors.toCollection(HashSet::new));

    // Check all tenant roles
    List<TenantRole> trs = userDto.getTenantRoles();
    for (TenantRole tr : trs) {
      if (!tenants.contains(tr.getTenant())) {
        throw new NotFoundException(
            "Failed to check tenant role", String.format("Bad tenant for tenant role '%s'", tr));
      }
    }

    // Check all access contracts exist
    List<TenantContract> acs = userDto.getAccessContracts();
    accessContractService.checkTenantContractExists(tenants, acs);

    // Check all ingest contracts exist
    List<TenantContract> ics = userDto.getIngestContracts();
    ingestContractService.checkTenantContractExists(tenants, ics);

    // Get entity from db
    UserDb userDb = getUserDbByIdentifier(organizationId, userIdentifier);
    UserDb oriUserDb = copyDtoToEntity(userDto, userDb);
    if (!userDb.getPassword().equals(oriUserDb.getPassword())) {
      // TODO Manage password (password seems to change at every update)
      oriUserDb.setPassword("********");
    }

    // Create operation
    OrganizationDb organizationDb = organizationService.getOrganizationDbById(organizationId);
    Long tenant = organizationDb.getTenant();
    OperationDb operation = updateOperation(tenant, userIdentifier, applicationId);
    operation.setProperty01(userDb.getId().toString());

    // Add LifeCycle
    JsonNode userNode = JsonService.toJson(toDto(userDb));
    JsonNode oriUserNode = JsonService.toJson(toDto(oriUserDb));
    JsonNode patchNode = JsonDiff.asJson(userNode, oriUserNode);
    String patch = JsonService.toString(patchNode);

    userDb.addLifeCycle(
        new LifeCycle(
            userDb.getAutoVersion(),
            operation.getId(),
            operation.getType(),
            operation.getCreated(),
            patch));
    userDb.incAutoVersion();
    userDb.setLastUpdate(operation.getCreated().toLocalDate());
    return toDto(userDb);
  }

  private OperationDb createOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op = OperationFactory.createUserOp(tenant, userIdentifier, applicationId);
    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Backuping users");
    return operationService.save(op);
  }

  private OperationDb updateOperation(Long tenant, String userIdentifier, String applicationId) {
    OperationDb op = OperationFactory.updateUserOp(tenant, userIdentifier, applicationId);
    op.setStatus(OperationStatus.BACKUP);
    op.setMessage("Backuping user");
    return operationService.save(op);
  }

  @Transactional
  public UserDto getUserDtoById(Long userId) {
    Assert.notNull(userId, "userId must be not null");

    return repository
        .findById(userId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    USER_NOT_FOUND, String.format("%s with id %s not found", ENTITY_NAME, userId)));
  }

  @Transactional
  public UserDto getUserDtoByIdentifier(Long organizationId, String userIdentifier) {
    Assert.notNull(organizationId, ORGANIZATION_ID_MUST_BE_NOT_NULL);
    Assert.hasText(userIdentifier, USER_IDENTIFIER_MUST_BE_NOT_NULL);

    return repository
        .findByIdentifierAndOrganizationId(userIdentifier, organizationId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    USER_NOT_FOUND,
                    String.format("%s with identifier %s not found", ENTITY_NAME, userIdentifier)));
  }

  @Transactional
  public UserDb getUserDbByIdentifier(Long organizationId, String userIdentifier) {
    Assert.notNull(organizationId, ORGANIZATION_ID_MUST_BE_NOT_NULL);
    Assert.hasText(userIdentifier, USER_IDENTIFIER_MUST_BE_NOT_NULL);

    return repository
        .findByIdentifierAndOrganizationId(userIdentifier, organizationId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    USER_NOT_FOUND,
                    String.format("%s with identifier %s not found", ENTITY_NAME, userIdentifier)));
  }

  @Transactional
  public List<UserDto> getUserDtosByTenant(Long tenant) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    Long organizationId = tenantDb.getOrganization().getId();
    return repository.findByOrganizationId(organizationId).stream().map(this::toDto).toList();
  }

  @Transactional
  public List<UserDto> getUserDtos(Long organizationId) {
    Assert.notNull(organizationId, ORGANIZATION_ID_MUST_BE_NOT_NULL);
    return repository.findByOrganizationId(organizationId).stream().map(this::toDto).toList();
  }
}
