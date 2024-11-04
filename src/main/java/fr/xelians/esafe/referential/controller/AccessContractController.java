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
 * Controller for managing access contracts in the system. This controller provides endpoints for
 * creating, retrieving, updating, and searching access contracts.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class AccessContractController {

  private final AccessContractService accessContractService;

  @PostMapping(V1 + ACCESS_CONTRACTS)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Create new access contracts")
  public ResponseEntity<List<AccessContractDto>> createAccessContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<AccessContractDto> accessContracts) {

    OperationDb operation = createOperation(OperationType.CREATE_ACCESSCONTRACT, tenant);
    List<AccessContractDto> dtos =
        accessContractService.createAccessContracts(operation, tenant, accessContracts);
    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + ACCESS_CONTRACTS);
    return ResponseEntity.created(location).headers(headers).body(dtos);
  }

  @GetMapping(V1 + ACCESS_CONTRACTS + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Find access contract by identifier")
  public AccessContractDto findAccessContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return accessContractService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + ACCESS_CONTRACTS)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Search access contracts")
  public SearchResult<AccessContractDto> searchAccessContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return accessContractService.search(tenant, query);
  }

  @PutMapping(V1 + ACCESS_CONTRACTS + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @Operation(summary = "Update access contract from identifier")
  public ResponseEntity<AccessContractDto> updateAccessContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody AccessContractDto accessContract) {

    OperationDb operation = createOperation(OperationType.UPDATE_ACCESSCONTRACT, tenant);
    AccessContractDto dto =
        accessContractService.updateAccessContract(operation, tenant, identifier, accessContract);

    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(dto);
  }

  @GetMapping(V2 + ACCESS_CONTRACTS + "/{identifier}")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Get access contract from identifier")
  public AccessContractDto getAccessContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return accessContractService.getDto(tenant, identifier);
  }

  @GetMapping(V2 + ACCESS_CONTRACTS)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Find access contracts")
  public PageResult<AccessContractDto> findAccessContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return accessContractService.getDtos(tenant, name, status, pageRequest);
  }

  @PostMapping(V2 + ACCESS_CONTRACTS + "/search")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @Operation(summary = "Search access contracts - V2")
  public SearchResult<AccessContractDto> searchAccessContractsV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return accessContractService.search(tenant, query);
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
