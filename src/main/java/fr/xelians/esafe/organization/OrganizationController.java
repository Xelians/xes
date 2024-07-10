/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_USER_READER;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.authentication.service.SignupService;
import fr.xelians.esafe.organization.dto.*;
import fr.xelians.esafe.organization.service.OrganizationService;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.organization.service.UserService;
import fr.xelians.esafe.processing.ProcessingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@ResponseStatus(HttpStatus.OK)
@RequiredArgsConstructor
public class OrganizationController {

  private final SignupService signupService;
  private final OrganizationService organizationService;
  private final TenantService tenantService;
  private final UserService userService;
  private final ProcessingService processingService;

  /*
   *   Register new organisation & user (not need to be authenticated)
   */
  @PostMapping(V1 + SIGNUP)
  public void signupRegister(@RequestBody SignupDto signupDto) {
    signupService.register(signupDto);
  }

  /*
   *   Create new organisation & user (not need to be authenticated)
   */
  @GetMapping(value = V1 + SIGNUP + "/{signupKey}", consumes = APPLICATION_JSON_VALUE)
  public SignupDto signupCreate(@PathVariable String signupKey) {
    return signupService.create(signupKey);
  }

  /*
   *   User Info
   */
  @GetMapping(value = V1 + ME, consumes = APPLICATION_JSON_VALUE)
  @Secured({ROLE_ADMIN, ROLE_USER_READER})
  public UserInfoDto getMe() {
    Long userId = AuthContext.getUserId();
    Long organizationId = AuthContext.getOrganizationId();
    OrganizationDto organizationDto = organizationService.getOrganizationDtoById(organizationId);
    UserDto userDto = userService.getUserDtoById(userId);
    return new UserInfoDto(organizationDto, userDto);
  }

  /*
   *   Organization
   */
  @PutMapping(V1 + ORGANIZATIONS)
  @Secured(ROLE_ADMIN)
  public OrganizationDto update(@RequestBody OrganizationDto organization) {
    Long organizationId = AuthContext.getOrganizationId();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return organizationService.update(organizationId, userIdentifier, applicationId, organization);
  }

  /*
   *   Tenant
   */
  @PostMapping(V1 + TENANTS)
  @Secured(ROLE_ADMIN)
  public List<TenantDto> createTenants(@RequestBody List<TenantDto> tenants) {
    Long organizationId = AuthContext.getOrganizationId();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return tenantService.create(organizationId, userIdentifier, applicationId, tenants);
  }

  @GetMapping(value = V1 + TENANT + "/{tenant}", consumes = APPLICATION_JSON_VALUE)
  @Secured(ROLE_ADMIN)
  public TenantDto getTenant(@PathVariable Long tenant) {
    Long organizationId = AuthContext.getOrganizationId();
    return tenantService.getTenant(organizationId, tenant);
  }

  @GetMapping(value = V1 + TENANTS, consumes = APPLICATION_JSON_VALUE)
  @Secured(ROLE_ADMIN)
  public List<TenantDto> getTenants() {
    Long organizationId = AuthContext.getOrganizationId();
    return tenantService.getTenantDtos(organizationId);
  }

  /*
   *   User
   */
  @PostMapping(V1 + USERS)
  @Secured(ROLE_ADMIN)
  public List<UserDto> createUsers(@RequestBody List<UserDto> users) {
    Long organizationId = AuthContext.getOrganizationId();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return userService.create(organizationId, userIdentifier, applicationId, users);
  }

  @PutMapping(V1 + USERS + "/{identifier}")
  @Secured(ROLE_ADMIN)
  public UserDto updateUser(@PathVariable String identifier, @RequestBody UserDto userDto) {
    Long organizationId = AuthContext.getOrganizationId();
    String applicationId = AuthContext.getApplicationId();
    return userService.update(organizationId, identifier, applicationId, userDto);
  }

  @GetMapping(value = V1 + USERS + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  @Secured(ROLE_ADMIN)
  public UserDto getUser(@PathVariable String identifier) {
    Long organizationId = AuthContext.getOrganizationId();
    return userService.getUserDtoByIdentifier(organizationId, identifier);
  }

  @GetMapping(value = V1 + USERS, consumes = APPLICATION_JSON_VALUE)
  @Secured(ROLE_ADMIN)
  public List<UserDto> getUsers() {
    Long organizationId = AuthContext.getOrganizationId();
    return userService.getUserDtos(organizationId);
  }
}
