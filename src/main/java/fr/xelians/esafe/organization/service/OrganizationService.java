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
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.repository.OrganizationRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

  public static final String ENTITY_NAME = "Organization";
  public static final String ID_NOT_FOUND = "%s with id %s not found";
  public static final String ORGANIZATION_NOT_FOUND = "Organization not found";

  private final OperationService operationService;
  private final OrganizationRepository repository;

  public OrganizationDto toDto(OrganizationDb entity) {
    return Utils.copyProperties(entity, new OrganizationDto());
  }

  public OrganizationDb toEntity(OrganizationDto dto) {
    return Utils.copyProperties(dto, new OrganizationDb());
  }

  // Internal use only
  @Transactional
  public OrganizationDb createDefault(OrganizationDto organizationDto) {
    Assert.notNull(organizationDto, String.format("%s dto cannot be null", ENTITY_NAME));

    OrganizationDb organizationDb = toEntity(organizationDto);
    organizationDb.setOperationId(-1L); // Fake value to avoid null constraint
    organizationDb.setCreationDate(LocalDate.now());
    organizationDb.setLastUpdate(organizationDb.getCreationDate());
    organizationDb.setAutoVersion(1);
    return repository.save(organizationDb);
  }

  @Transactional
  public OrganizationDto update(
      Long organizationId,
      String userIdentifier,
      String applicationId,
      OrganizationDto organizationDto) {
    Assert.notNull(organizationDto, String.format("%s dto cannot be null", ENTITY_NAME));

    // Get organization from db
    OrganizationDb organizationDb = getOrganizationDbById(organizationId);

    if (organizationDto.getIdentifier() != null
        && !organizationDb.getIdentifier().equals(organizationDto.getIdentifier())) {
      throw new BadRequestException(
          "Entity update failed",
          String.format(
              "Entity %s identifiers mismatch: %s vs %s",
              ENTITY_NAME, organizationDb.getIdentifier(), organizationDto.getIdentifier()));
    }

    OrganizationDb oriOrganizationDb = copyDtoToEntity(organizationDto, organizationDb);

    // Create operation
    OperationDb operation =
        OperationFactory.updateOrganizationOp(
            organizationDb.getTenant(), userIdentifier, applicationId);

    operation.setStatus(OperationStatus.BACKUP);
    operation.setMessage("Backuping organization");
    operation = operationService.save(operation);

    //    operation.setProperty01(organizationDb.getId().toString());

    // Add LifeCycle
    JsonNode organizationNode = JsonService.toJson(toDto(organizationDb));
    JsonNode oriOrganizationNode = JsonService.toJson(toDto(oriOrganizationDb));
    JsonNode patchNode = JsonDiff.asJson(organizationNode, oriOrganizationNode);
    String patch = JsonService.toString(patchNode);
    organizationDb.addLifeCycle(
        new LifeCycle(
            organizationDb.getAutoVersion(),
            operation.getId(),
            operation.getType(),
            operation.getCreated(),
            patch));
    organizationDb.incAutoVersion();
    organizationDb.setLastUpdate(operation.getCreated().toLocalDate());
    return toDto(organizationDb);
  }

  private OrganizationDb copyDtoToEntity(
      OrganizationDto organizationDto, OrganizationDb organizationDb) {
    // Keep off non-updatable fields
    OrganizationDb oriOrganizationDb = Utils.copyProperties(organizationDb, new OrganizationDb());
    Utils.copyProperties(organizationDto, organizationDb);
    organizationDb.setCreationDate(oriOrganizationDb.getCreationDate());
    organizationDb.setLastUpdate(oriOrganizationDb.getLastUpdate());
    organizationDb.setOperationId(oriOrganizationDb.getOperationId());
    organizationDb.setAutoVersion(oriOrganizationDb.getAutoVersion());
    organizationDb.setLfcs(oriOrganizationDb.getLfcs());
    return oriOrganizationDb;
  }

  public OrganizationDto getOrganizationDtoById(Long organizationId) {
    return repository
        .findById(organizationId)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    ORGANIZATION_NOT_FOUND,
                    String.format(ID_NOT_FOUND, ENTITY_NAME, organizationId)));
  }

  public OrganizationDto getOrganizationDtoByTenant(Long tenant) {
    return repository
        .findByTenant(tenant)
        .map(this::toDto)
        .orElseThrow(
            () ->
                new NotFoundException(
                    ORGANIZATION_NOT_FOUND, String.format(ID_NOT_FOUND, ENTITY_NAME, tenant)));
  }

  public OrganizationDb getOrganizationDbById(Long organizationId) {
    return repository
        .findById(organizationId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    ORGANIZATION_NOT_FOUND,
                    String.format(ID_NOT_FOUND, ENTITY_NAME, organizationId)));
  }

  // Internal Use only
  public boolean existsOrganizationByIdentifier(String identifier) {
    return repository.existsByIdentifier(identifier);
  }

  public void deleteOrganization(OrganizationDb entity) {
    repository.delete(entity);
  }
}
