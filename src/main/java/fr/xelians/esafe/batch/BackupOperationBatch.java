/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.batch;

import static java.util.stream.Collectors.groupingBy;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.service.ServerNodeService;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.OrganizationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.organization.service.UserService;
import fr.xelians.esafe.referential.dto.*;
import fr.xelians.esafe.referential.service.*;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.ChecksumStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// All methods in this class must be idempotent
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupOperationBatch {

  private final ServerNodeService serverNodeService;
  private final OperationService operationService;
  private final ProfileService profileService;
  private final AgencyService agencyService;
  private final RuleService ruleService;
  private final AccessContractService accessContractService;
  private final IngestContractService ingestContractService;
  private final OntologyService ontologyService;
  private final UserService userService;
  private final OrganizationService organizationService;
  private final StorageService storageService;
  private final TenantService tenantService;
  private final LogbookService logbookService;

  @Scheduled(
      fixedDelayString = "${app.batch.backup.operation.fixedDelay:2000}",
      initialDelayString = "${app.batch.backup.operation.initialDelay:2000}")
  public void run() {
    try {
      if (serverNodeService.hasFeature(NodeFeature.BACKUP)) {
        operationService.findByStatus(OperationStatus.BACKUP).stream()
            .collect(groupingBy(OperationDb::getTenant))
            .values()
            .forEach(this::selectOperation);
      }
    } catch (Exception ex) {
      log.error("Store batch failed", ex);
    }
  }

  private void selectOperation(List<OperationDb> operations) {
    Long tenant = operations.get(0).getTenant();
    TenantDb tenantDb = tenantService.getTenantDb(tenant);

    List<OperationDb> agencyOperations = new ArrayList<>();
    List<OperationDb> profileOperations = new ArrayList<>();
    List<OperationDb> ruleOperations = new ArrayList<>();
    List<OperationDb> ontologyOperations = new ArrayList<>();
    List<OperationDb> accesscontractOperations = new ArrayList<>();
    List<OperationDb> ingestcontractOperations = new ArrayList<>();
    List<OperationDb> userOperations = new ArrayList<>();
    List<OperationDb> tenantOperations = new ArrayList<>();
    List<OperationDb> orgaOperations = new ArrayList<>();

    for (OperationDb operation : operations) {
      switch (operation.getType()) {
        case CREATE_ACCESSCONTRACT, UPDATE_ACCESSCONTRACT -> accesscontractOperations.add(
            operation);
        case CREATE_AGENCY, UPDATE_AGENCY -> agencyOperations.add(operation);
        case CREATE_ONTOLOGY, UPDATE_ONTOLOGY -> ontologyOperations.add(operation);
        case CREATE_INGESTCONTRACT, UPDATE_INGESTCONTRACT -> ingestcontractOperations.add(
            operation);
        case CREATE_ORGANIZATION, UPDATE_ORGANIZATION -> orgaOperations.add(operation);
        case CREATE_PROFILE, UPDATE_PROFILE -> profileOperations.add(operation);
        case CREATE_RULE, UPDATE_RULE -> ruleOperations.add(operation);
        case CREATE_TENANT, UPDATE_TENANT -> tenantOperations.add(operation);
        case CREATE_USER, UPDATE_USER -> userOperations.add(operation);
        default -> throw new InternalException(
            "Select operation batch failed",
            String.format(
                "Bad operation '%s' - type '%s'", operation.getId(), operation.getType()));
      }
    }

    if (!accesscontractOperations.isEmpty()) {
      List<AccessContractDto> accessContracts = accessContractService.getDtos(tenant);
      backupOperations(accesscontractOperations, tenantDb, accessContracts, StorageObjectType.acc);
    }

    if (!agencyOperations.isEmpty()) {
      List<AgencyDto> agencies = agencyService.getDtos(tenant);
      backupOperations(agencyOperations, tenantDb, agencies, StorageObjectType.age);
    }

    if (!ontologyOperations.isEmpty()) {
      List<OntologyDto> ontologies = ontologyService.getDtos(tenant);
      backupOperations(ontologyOperations, tenantDb, ontologies, StorageObjectType.ind);
    }

    if (!ingestcontractOperations.isEmpty()) {
      List<IngestContractDto> ingestContracts = ingestContractService.getDtos(tenant);
      backupOperations(ingestcontractOperations, tenantDb, ingestContracts, StorageObjectType.ing);
    }

    if (!orgaOperations.isEmpty()) {
      // TODO Maybe we should use a specific DTO for backup to access internal state
      OrganizationDto organization = organizationService.getOrganizationDtoByTenant(tenant);
      backupOperations(orgaOperations, tenantDb, organization, StorageObjectType.org);
    }

    if (!profileOperations.isEmpty()) {
      List<ProfileDto> profiles = profileService.getDtos(tenant);
      backupOperations(profileOperations, tenantDb, profiles, StorageObjectType.pro);
    }

    if (!ruleOperations.isEmpty()) {
      List<RuleDto> rules = ruleService.getDtos(tenant);
      backupOperations(profileOperations, tenantDb, rules, StorageObjectType.rul);
    }

    if (!tenantOperations.isEmpty()) {
      // TODO Maybe we should use a specific DTO for backup to access internal state
      // TODO Backup the secret keys ? note. it's irrelvant to save keys on encrypted offers...
      // instead we should backup all keys in a HSM (with KMIP)
      // We don't backup the secureNumber. It could be retrieved from tenant offer when rebuilding
      // DB
      backupOperations(tenantOperations, tenantDb, tenantDb, StorageObjectType.ten);
    }

    if (!userOperations.isEmpty()) {
      // TODO Maybe we should use a specific DTO for backup to access internal state (I.e. passwod)
      List<UserDto> users = userService.getUserDtosByTenant(tenant);
      backupOperations(userOperations, tenantDb, users, StorageObjectType.usr);
    }
  }

  private void backupOperations(
      List<OperationDb> operations,
      TenantDb tenantDb,
      Object entity,
      StorageObjectType objectType) {

    List<StorageObject> storageObjects = new ArrayList<>();
    byte[] bytes = JsonService.toBytes(entity, JsonConfig.DEFAULT);
    storageObjects.add(new ByteStorageObject(bytes, 1L, objectType));
    doBackup(operations, tenantDb, storageObjects);
  }

  private void backupOperations(
      List<OperationDb> operations,
      TenantDb tenantDb,
      List<?> entities,
      StorageObjectType objectType) {

    List<StorageObject> storageObjects = new ArrayList<>();
    byte[] bytes = JsonService.collToBytes(entities, JsonConfig.DEFAULT);
    storageObjects.add(new ByteStorageObject(bytes, 1L, objectType));
    doBackup(operations, tenantDb, storageObjects);
  }

  private void doBackup(
      List<OperationDb> operations, TenantDb tenantDb, List<StorageObject> bsois) {

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      Long tenant = tenantDb.getId();
      List<String> offers = tenantDb.getStorageOffers();
      ChecksumStorageObject csoi = storageDao.putStorageObjects(tenant, offers, bsois).get(0);
      for (OperationDb operation : operations) {
        ActionType actionType = getActionType(operation);
        operation.addAction(StorageAction.create(actionType, csoi));
        logbookService.indexOperation(operation);
      }
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private ActionType getActionType(OperationDb operation) {
    return operation.getType().name().startsWith("CREATE") ? ActionType.CREATE : ActionType.UPDATE;
  }
}
