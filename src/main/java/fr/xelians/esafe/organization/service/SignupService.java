/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.service;

import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.domain.Root;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.entity.OrganizationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.entity.UserDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

  private static final String SIGNUP_ENTITY = "SignupOrganization";
  public static final String FAILED_TO_SIGNUP = "Failed to signup";

  private final OrganizationService organizationService;
  private final TenantService tenantService;
  private final UserService userService;
  private final OperationService operationService;

  protected String getEntityName() {
    return SIGNUP_ENTITY;
  }

  @Transactional
  public SignupDto create(SignupDto signupDto, String applicationId) {
    Assert.notNull(signupDto, String.format("%s dto cannot be null", getEntityName()));
    Assert.notNull(signupDto.getOrganizationDto(), "Organization dto cannot be null");
    Assert.notNull(signupDto.getUserDto(), "User dto cannot be null");
    Assert.isTrue(!signupDto.getUserDto().isRootAdmin(), "User cannot have root admin role");

    OrganizationDto organizationDto = signupDto.getOrganizationDto();
    String organizationIdentifier = organizationDto.getIdentifier();

    if (Root.ORGA_IDENTIFIER.equals(organizationIdentifier)) {
      throw new BadRequestException(
          FAILED_TO_SIGNUP,
          String.format(
              "Failed to create organization because identifier '%s' not allowed",
              Root.ORGA_IDENTIFIER));
    }

    UserDto userDto = signupDto.getUserDto();
    String userIdentifier = userDto.getIdentifier();

    if (Root.USER_IDENTIFIER.equals(userIdentifier)) {
      throw new BadRequestException(
          FAILED_TO_SIGNUP,
          String.format(
              "Failed to create user because identifier '%s' not allowed", Root.USER_IDENTIFIER));
    }

    if (Root.ORGA_IDENTIFIER.equals(userDto.getOrganizationIdentifier())) {
      throw new BadRequestException(
          FAILED_TO_SIGNUP,
          String.format(
              "Failed to create user because organization identifier '%s' not allowed",
              Root.ORGA_IDENTIFIER));
    }

    TenantDto tenantDto = signupDto.getTenantDto();
    if (Root.ORGA_IDENTIFIER.equals(tenantDto.getOrganizationIdentifier())) {
      throw new BadRequestException(
          FAILED_TO_SIGNUP,
          String.format(
              "Failed to create tenant because organization identifier '%s' not allowed",
              Root.ORGA_IDENTIFIER));
    }

    return doCreate(signupDto, applicationId);
  }

  @Transactional
  public SignupDto createRoot(SignupDto signupDto) {
    Assert.notNull(signupDto, String.format("%s dto cannot be null", getEntityName()));
    Assert.notNull(signupDto.getOrganizationDto(), "Organization dto cannot be null");
    Assert.notNull(signupDto.getUserDto(), "User dto cannot be null");
    Assert.isTrue(signupDto.getUserDto().isRootAdmin(), "User must have root admin role");

    return doCreate(signupDto, "RootAdminInit");
  }

  private SignupDto doCreate(SignupDto signupDto, String applicationId) {

    OrganizationDto organizationDto = signupDto.getOrganizationDto();
    String organizationIdentifier = organizationDto.getIdentifier();

    if (organizationService.existsOrganizationByIdentifier(organizationIdentifier)) {
      throw new BadRequestException(
          "Register organization failed",
          "Failed to register organization because identifier already exists");
    }

    // Create organization
    OrganizationDb organizationDb = organizationService.create(organizationDto);

    // Create tenant
    TenantDto tenantDto = signupDto.getTenantDto();
    TenantDb tenantDb = tenantService.createEntity(tenantDto, organizationDb);
    Long tenant = tenantDb.getId();

    // Create admin user
    UserDto userDto = signupDto.getUserDto();
    UserDb userDb = userService.createEntity(userDto, organizationDb);
    String userIdentifier = userDb.getIdentifier();

    // Create organization operation
    OperationDb orgaOp =
        OperationFactory.createOrganizationOp(tenant, userIdentifier, applicationId);
    orgaOp.setStatus(OperationStatus.BACKUP);
    orgaOp.setMessage("Backuping organization");

    OperationDb orgaOperation = operationService.save(orgaOp);
    organizationDb.setOperationId(orgaOperation.getId());
    organizationDb.setTenant(orgaOperation.getTenant());

    // Create tenant operation
    OperationDb tenantOp = OperationFactory.createTenantOp(tenant, userIdentifier, applicationId);
    tenantOp.setStatus(OperationStatus.BACKUP);
    tenantOp.setMessage("Backuping tenant");

    OperationDb tenantOperation = operationService.save(tenantOp);
    tenantDb.setOperationId(tenantOperation.getId());
    tenantOperation.setProperty01(tenantDb.getId().toString());

    // Create user operation
    OperationDb userOp = OperationFactory.createUserOp(tenant, userIdentifier, applicationId);
    userOp.setStatus(OperationStatus.BACKUP);
    userOp.setMessage("Backuping user");

    OperationDb userOperation = operationService.save(userOp);
    userDb.setOperationId(userOperation.getId());
    userOperation.setProperty01(userDb.getId().toString());

    // Create Signup
    SignupDto sigDto = new SignupDto();
    sigDto.setOrganizationDto(organizationService.toDto(organizationDb));
    sigDto.setUserDto(userService.toDto(userDb));
    sigDto.setTenantDto(tenantService.toDto(tenantDb));
    return sigDto;
  }
}
