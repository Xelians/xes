/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_MANAGER;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_READER;
import static org.springframework.http.MediaType.*;

import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.domain.SortDir;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.referential.domain.SortBy;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.dto.*;
import fr.xelians.esafe.referential.service.*;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * AgencyController is a REST controller responsible for handling agency-related operations. This
 * controller provides endpoints for creating, updating, deleting, and searching agencies in both
 * CSV and JSON formats. It manages requests for two different API versions (V1 and V2).
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class AgencyController {

  private final AgencyService agencyService;

  @PostMapping(value = V1 + AGENCIES, consumes = APPLICATION_OCTET_STREAM_VALUE)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Create Agency from CSV")
  public ResponseEntity<List<AgencyDto>> createAgencyCsvWithOctetStreamConsumer(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody String csv) {

    validateCsvInput(csv);

    OperationDb operation = createOperation(OperationType.CREATE_AGENCY, tenant);
    List<AgencyDto> agencyDtos = agencyService.createCsvAgencies(operation, tenant, csv);

    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + AGENCIES);
    // It differs from Vitam by returning the created agencies in the body
    return ResponseEntity.created(location).headers(headers).body(agencyDtos);
  }

  @GetMapping(V1 + AGENCIES + "/{identifier}/csv")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(
      summary = "Find Agencies by Identifier. Returns a CSV",
      operationId = "findAgencyByIdentifierCsv")
  public String findAgencyByIdentifierCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getAgenciesCsv(tenant, identifier);
  }

  @GetMapping(V1 + AGENCIES + "/csv")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Find All Agencies (CSV)", operationId = "findAgenciesCsv")
  public String findAgenciesCsv(@RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant) {
    return agencyService.getAgenciesCsv(tenant);
  }

  // TODO Use PageResult !
  @GetMapping(V1 + AGENCIES + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Find Agency by Identifier", operationId = "findAgencyByIdentifier")
  public AgencyDto findAgencyByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + AGENCIES)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Search Agencies", operationId = "searchAgencies")
  public SearchResult<AgencyDto> searchAgencies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return agencyService.search(tenant, query);
  }

  // V2

  @PostMapping(V2 + AGENCIES)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Create Agencies from json", operationId = "createAgency")
  public ResponseEntity<List<AgencyDto>> createAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<AgencyDto> agencies) {

    OperationDb operation = createOperation(OperationType.CREATE_AGENCY, tenant);
    List<AgencyDto> agencyDtos = agencyService.createAgencies(operation, tenant, agencies);
    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + AGENCIES);
    return ResponseEntity.created(location).headers(headers).body(agencyDtos);
  }

  // We expect a more logical media consumer than the V1 createAgency endpoint
  @PostMapping(value = V2 + AGENCIES + "/csv", consumes = TEXT_PLAIN_VALUE)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Create Agency from CSV", operationId = "createAgencyCsv")
  public ResponseEntity<List<AgencyDto>> createAgencyCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody String csv) {

    validateCsvInput(csv);

    OperationDb operation = createOperation(OperationType.CREATE_AGENCY, tenant);
    List<AgencyDto> agencyDtos = agencyService.createCsvAgencies(operation, tenant, csv);

    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + AGENCIES);
    // It differs from Vitam by returning the created agencies in the body
    return ResponseEntity.created(location).headers(headers).body(agencyDtos);
  }

  @GetMapping(V2 + AGENCIES + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Get Agency by Identifier", operationId = "getAgencyByIdentifier")
  public AgencyDto getAgencyByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getDto(tenant, identifier);
  }

  @GetMapping(V2 + AGENCIES)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Find Agencies")
  public PageResult<AgencyDto> findAgencies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return agencyService.getDtos(tenant, name, status, pageRequest);
  }

  @PutMapping(V2 + AGENCIES + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Update Agency", operationId = "updateAgency")
  public ResponseEntity<AgencyDto> updateAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody AgencyDto agency) {

    OperationDb operation = createOperation(OperationType.UPDATE_AGENCY, tenant);
    AgencyDto updatedAgency = agencyService.updateAgency(operation, tenant, identifier, agency);
    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(updatedAgency);
  }

  @DeleteMapping(V2 + AGENCIES + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Delete Agency", operationId = "deleteAgency")
  public ResponseEntity<Void> deleteAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {

    OperationDb operation = createOperation(OperationType.DELETE_AGENCY, tenant);
    agencyService.deleteAgency(operation, tenant, identifier);
    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).build();
  }

  @PostMapping(V2 + AGENCIES + "/search")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Search Agencies", operationId = "searchAgenciesV2")
  public SearchResult<AgencyDto> searchAgenciesV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return agencyService.search(tenant, query);
  }

  private void validateCsvInput(String csv) {
    if (csv.length() > 1_000_000) {
      throw new BadRequestException("Agency creation failed", "Agency Csv is too big");
    }
    if (Utils.isNotHtmlSafe(csv)) {
      throw new BadRequestException("Agency creation failed", "Agency Csv contains html");
    }
  }

  private OperationDb createOperation(OperationType operationType, Long tenant) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return OperationFactory.createReferentialOp(
        operationType, tenant, userIdentifier, applicationId);
  }

  private HttpHeaders createHeaders(OperationDb operation) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operation.getId().toString());
    return headers;
  }
}
