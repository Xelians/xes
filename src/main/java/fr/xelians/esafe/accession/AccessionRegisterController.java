/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_TENANT_ID;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_READER;

import fr.xelians.esafe.accession.dto.RegisterDetailsDto;
import fr.xelians.esafe.accession.dto.RegisterDto;
import fr.xelians.esafe.accession.service.AccessionRegisterService;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The {@code AccessionRegisterController} class provides REST API endpoints for searching and
 * retrieving accession registers, both in summary and detailed form. These endpoints allow users to
 * search for registers based on specific search queries and retrieve detailed information for
 * individual registers.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
@Validated
public class AccessionRegisterController {

  private final AccessionRegisterService accessionRegisterService;

  @Operation(summary = "Search for accession registers")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V1 + ACCESSION_REGISTER_SUMMARY)
  public SearchResult<RegisterDto> searchAccessionRegisterSummary(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return accessionRegisterService.searchSummary(tenant, query);
  }

  @Operation(summary = "Search for accession registers")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V1 + ACCESSION_REGISTER_SYMBOLIC)
  public SearchResult<RegisterDto> searchAccessionRegisterSymbolic(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return accessionRegisterService.searchSymbolic(tenant, query);
  }

  @Operation(summary = "Get one detailed accession registers")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + ACCESSION_REGISTERS + "/{id}" + ACCESSION_REGISTER_DETAILS)
  public SearchResult<RegisterDetailsDto> getAccessionRegisterDetails(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long id) throws IOException {
    return accessionRegisterService.searchDetails(tenant, id);
  }

  @Operation(summary = "Search for detailed accession registers")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V1 + ACCESSION_REGISTER_DETAILS)
  public SearchResult<RegisterDetailsDto> searchAccessionRegisterDetails(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return accessionRegisterService.searchDetails(tenant, query);
  }
}
