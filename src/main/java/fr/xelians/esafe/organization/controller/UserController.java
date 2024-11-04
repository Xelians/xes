/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ADMIN;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_READER;

import fr.xelians.esafe.organization.accesskey.AccessKeyDto;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.dto.UserInfoDto;
import fr.xelians.esafe.organization.service.AccessKeyService;
import fr.xelians.esafe.organization.service.OrganizationService;
import fr.xelians.esafe.organization.service.UserService;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The {@code UserController} class provides REST API endpoints for managing users and organizations
 * in the system. These operations include creating, updating, and retrieving user details, as well
 * as generating access keys for secure backend operations.
 *
 * @author Emmanuel Deviller
 */
@RestController
@RequestMapping(V1 + USERS)
@RequiredArgsConstructor
public class UserController {

  private final OrganizationService organizationService;
  private final UserService userService;
  private final AccessKeyService accessKeyService;

  @Operation(summary = "Create new users")
  @PostMapping
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public List<UserDto> createUsers(@RequestBody List<UserDto> users) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return userService.create(organizationIdentifier, userIdentifier, applicationId, users);
  }

  @Operation(summary = "Update user from identifier")
  @PutMapping("/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public UserDto updateUser(@PathVariable String identifier, @RequestBody UserDto userDto) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return userService.update(organizationIdentifier, identifier, applicationId, userDto);
  }

  @Operation(summary = "Get user from identifier")
  @GetMapping("/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public UserDto getUser(@PathVariable String identifier) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    return userService.getUserDtoByIdentifier(organizationIdentifier, identifier);
  }

  @Operation(summary = "Find all users in the organization")
  @GetMapping
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public List<UserDto> getUsers() {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    return userService.getUserDtos(organizationIdentifier);
  }

  @Operation(summary = "Get my detailed information")
  @GetMapping(value = ME)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  public UserInfoDto getMe() {
    String userIdentifier = AuthContext.getUserIdentifier();
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    OrganizationDto organizationDto =
        organizationService.getOrganizationDtoByIdentifier(organizationIdentifier);
    UserDto userDto = userService.getUserDtoByIdentifier(organizationIdentifier, userIdentifier);
    return new UserInfoDto(organizationDto, userDto);
  }

  @Operation(
      summary =
          "Create access key for current user. The access token is for use exclusively in a secure backend flow.")
  @GetMapping(USER_TOKEN)
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public AccessKeyDto createToken() {
    String organizationId = AuthContext.getOrganizationIdentifier();
    String userIdentifier = AuthContext.getUserIdentifier();
    return accessKeyService.createToken(organizationId, userIdentifier);
  }
}
