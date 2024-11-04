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

import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.domain.SortDir;
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
 * The {@code IngestContractController} class is a REST controller that manages ingest contracts in
 * the system. It provides various endpoints for creating, updating, searching, and retrieving
 * ingest contracts. Access to these endpoints is governed by role-based access control, with
 * specific roles required to perform certain operations.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class IngestContractController {

  private final IngestContractService ingestContractService;

  @Operation(summary = "Create Ingest Contracts")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(V1 + INGEST_CONTRACTS)
  public ResponseEntity<List<IngestContractDto>> createIngestContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<IngestContractDto> ingestContracts) {

    OperationDb operation = createOperation(OperationType.CREATE_INGESTCONTRACT, tenant);
    List<IngestContractDto> dtos =
        ingestContractService.createIngestContracts(operation, tenant, ingestContracts);
    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS);
    return ResponseEntity.created(location).headers(headers).body(dtos);
  }

  // TODO Use PageResult !
  @Operation(summary = "Find Ingest Contract By Id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + INGEST_CONTRACTS + "/{identifier}")
  public IngestContractDto findIngestContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ingestContractService.getDto(tenant, identifier);
  }

  @Operation(summary = "Search Ingest Contracts")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + INGEST_CONTRACTS)
  public SearchResult<IngestContractDto> searchIngestContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ingestContractService.search(tenant, query);
  }

  @Operation(summary = "Update Ingest Contract")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(V1 + INGEST_CONTRACTS + "/{identifier}")
  public ResponseEntity<IngestContractDto> updateIngestContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody IngestContractDto ingestContract) {

    OperationDb operation = createOperation(OperationType.UPDATE_INGESTCONTRACT, tenant);
    IngestContractDto dto =
        ingestContractService.updateIngestContract(operation, tenant, identifier, ingestContract);
    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(dto);
  }

  @Operation(summary = "Search Ingest Contracts")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V2 + INGEST_CONTRACTS + "/search")
  public SearchResult<IngestContractDto> searchIngestContractsV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ingestContractService.search(tenant, query);
  }

  @Operation(summary = "Get Ingest Contract By Identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + INGEST_CONTRACTS + "/{identifier}")
  public IngestContractDto getIngestContractByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ingestContractService.getDto(tenant, identifier);
  }

  @Operation(summary = "Find Ingest Contracts")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + INGEST_CONTRACTS)
  public PageResult<IngestContractDto> findIngestContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return ingestContractService.getDtos(tenant, name, status, pageRequest);
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
