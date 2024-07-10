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

package fr.xelians.esafe.authentication.service;

import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.functional.ForbiddenException;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.organization.service.OrganizationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.organization.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

  // FIX this big bug: it does not work in a clustered environnement -> used the DB
  private static final Map<String, SignupDto> signupMap = new PassiveExpiringMap<>(60_000L * 5);
  private static final String SIGNUP_ENTITY = "SignupOrganization";

  private final OrganizationService organizationService;
  private final TenantService tenantService;
  private final UserService userService;
  private final OperationService operationService;

  protected String getEntityName() {
    return SIGNUP_ENTITY;
  }

  private String getRandomSignupKey(String salt) {
    // TODO This is done only for testing purpose. Use random in production
    return salt;
    // return RandomStringUtils.randomAlphanumeric(256);
  }

  public void register(SignupDto signupDto) {
    Assert.notNull(signupDto, String.format("%s dto cannot be null", getEntityName()));
    Assert.notNull(signupDto.getOrganizationDto(), "Organization dto cannot be null");
    Assert.notNull(signupDto.getUserDto(), "User dto cannot be null");

    OrganizationDto organizationDto = signupDto.getOrganizationDto();
    String identifier = organizationDto.getIdentifier();

    // TODO fix - use Db (besides do not lock on this map!)
    synchronized (signupMap) {
      if (organizationService.existsOrganizationByIdentifier(identifier)) {
        throw new BadRequestException(
            "Register organization failed",
            "Failed to register organization because identifier already exists");
      }

      if (signupMap.size() > 10000) {
        throw new BadRequestException(
            "Register organization failed", "Too many registered organizations waiting for signup");
      }

      // TODO fix
      String salt = identifier;
      String key = getRandomSignupKey(salt);
      signupMap.put(key, signupDto);
      // TODO: Send mail to create organization with the key
    }
  }

  @Transactional
  public SignupDto create(String signupKey) {
    Assert.notNull(signupKey, String.format("%s signup key cannot be null", signupKey));
    Assert.hasText(signupKey, String.format("%s signup key cannot be empty", signupKey));

    SignupDto registeredDto;
    synchronized (signupMap) {
      registeredDto = signupMap.get(signupKey);
    }
    if (registeredDto == null) {
      throw new ForbiddenException(
          "Signup creation failed", String.format("Signup key %s not found", signupKey));
    }

    // TODO get ApplicationId from Context
    String applicationId = "";

    // Create organization
    OrganizationDb organizationDb =
        organizationService.createDefault(registeredDto.getOrganizationDto());

    // Create tenant
    TenantDb tenantDb = tenantService.createEntity(registeredDto.getTenantDto(), organizationDb);

    // Create admin user
    UserDb userDb = userService.createEntity(registeredDto.getUserDto(), organizationDb, tenantDb);
    String userIdentifier = userDb.getIdentifier();

    // Create organization operation
    OperationDb orgaOp =
        OperationFactory.createOrganizationOp(tenantDb.getId(), userIdentifier, applicationId);
    orgaOp.setStatus(OperationStatus.BACKUP);
    orgaOp.setMessage("Backuping organization");

    OperationDb orgaOperation = operationService.save(orgaOp);
    organizationDb.setOperationId(orgaOperation.getId());
    organizationDb.setTenant(orgaOperation.getTenant());

    // Create tenant operation
    OperationDb tenantOp =
        OperationFactory.createTenantOp(tenantDb.getId(), userIdentifier, applicationId);
    tenantOp.setStatus(OperationStatus.BACKUP);
    tenantOp.setMessage("Backuping tenant");

    OperationDb tenantOperation = operationService.save(tenantOp);
    tenantDb.setOperationId(tenantOperation.getId());
    tenantOperation.setProperty01(tenantDb.getId().toString());

    // Create user operation
    OperationDb userOp =
        OperationFactory.createUserOp(tenantDb.getId(), userIdentifier, applicationId);
    userOp.setStatus(OperationStatus.BACKUP);
    userOp.setMessage("Backuping user");

    OperationDb userOperation = operationService.save(userOp);
    userDb.setOperationId(userOperation.getId());
    userOperation.setProperty01(userDb.getId().toString());

    // Create Signup
    SignupDto signupDto = new SignupDto();
    signupDto.setOrganizationDto(organizationService.toDto(organizationDb));
    signupDto.setUserDto(userService.toDto(userDb));
    signupDto.setTenantDto(tenantService.toDto(tenantDb));

    return signupDto;
  }
}
