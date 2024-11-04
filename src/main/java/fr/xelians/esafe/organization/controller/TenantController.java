/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ADMIN;

import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/*
 * @author Emmanuel Deviller
 */
@RestController
@RequestMapping(V1 + TENANTS)
@RequiredArgsConstructor
public class TenantController {

  private final TenantService tenantService;

  @Operation(summary = "Create new tenants")
  @PostMapping
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public List<TenantDto> createTenants(@RequestBody List<TenantDto> tenants) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return tenantService.create(organizationIdentifier, userIdentifier, applicationId, tenants);
  }

  @Operation(summary = "Get the detailed tenant")
  @GetMapping("/{tenant}")
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public TenantDto getTenant(@PathVariable Long tenant) {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    return tenantService.getTenant(organizationIdentifier, tenant);
  }

  @Operation(summary = "Find all tenants in the organization")
  @GetMapping
  @PreAuthorize("hasRole('" + ROLE_ADMIN + "')")
  public List<TenantDto> getTenants() {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();
    return tenantService.getTenantDtos(organizationIdentifier);
  }
}
