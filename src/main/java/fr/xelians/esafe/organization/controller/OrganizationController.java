/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ADMIN;

import fr.xelians.esafe.organization.dto.*;
import fr.xelians.esafe.organization.service.OrganizationService;
import fr.xelians.esafe.organization.service.SignupService;
import fr.xelians.esafe.security.HeaderUtils;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@ResponseStatus(HttpStatus.OK)
@RequiredArgsConstructor
public class OrganizationController {

  private final SignupService signupService;
  private final OrganizationService organizationService;

  /*
   *  Create a new organization & user
   *
   *  Note. signup is excluded from the swagger config to avoid customization et authentification.
   *  If you change the method name, don't forget to update the swagger config.
   */
  @Operation(summary = "Create a new organization with user and tenant")
  @PostMapping(V1 + SIGNUP)
  public SignupDto signup(
      @RequestBody SignupDto signupDto,
      @RequestHeader(value = X_APPLICATION_ID, required = false) String appId) {
    return signupService.create(signupDto, HeaderUtils.getApplicationId(appId));
  }

  @Operation(summary = "Update the organization")
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  @PutMapping(V1 + ORGANIZATIONS)
  public OrganizationDto update(@RequestBody OrganizationDto organization) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return organizationService.update(
        organizationIdentifier, userIdentifier, applicationId, organization);
  }
}
