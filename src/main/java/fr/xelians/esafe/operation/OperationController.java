/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import fr.xelians.esafe.common.domain.SortDir;
import fr.xelians.esafe.common.utils.SearchUtils;
import fr.xelians.esafe.common.utils.SliceResult;
import fr.xelians.esafe.operation.domain.SortBy;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationQuery;
import fr.xelians.esafe.operation.dto.OperationResult;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.dto.vitam.VitamOperationDto;
import fr.xelians.esafe.operation.dto.vitam.VitamOperationListDto;
import fr.xelians.esafe.operation.service.OperationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class OperationController {

  private final OperationService operationService;

  // Vitam Operations
  @Operation(summary = "Get VITAM operation. Use only if you need VITAM compatibility.")
  @GetMapping(V1 + OPERATIONS_ID)
  public OperationResult<VitamOperationDto> getVitamOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId) {
    return operationService.getVitamOperationDto(tenant, operationId);
  }

  @Operation(
      summary = "Search for VITAM operations with (GET). Use only if you need VITAM compatibility.",
      hidden = true)
  @GetMapping(V1 + OPERATIONS)
  public OperationResult<VitamOperationListDto> searchVitamOperationsWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody OperationQuery operationQuery) {
    return operationService.searchVitamOperationDtos(tenant, operationQuery);
  }

  @Operation(summary = "Search for VITAM operations. Use only if you need VITAM compatibility.")
  @PostMapping(V1 + OPERATIONS)
  public OperationResult<VitamOperationListDto> searchVitamOperations(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody OperationQuery operationQuery) {
    return operationService.searchVitamOperationDtos(tenant, operationQuery);
  }

  // Operations
  @Operation(summary = "Get operation")
  @GetMapping(V2 + OPERATIONS_ID)
  public OperationDto getOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId) {
    return operationService.getOperationDto(tenant, operationId);
  }

  @Operation(summary = "Search for operations")
  @PostMapping(V2 + OPERATIONS)
  public SliceResult<OperationDto> searchOperations(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestBody OperationQuery operationQuery,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "id") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return operationService.searchOperationDtos(tenant, operationQuery, pageRequest);
  }

  // Operations status
  @Operation(summary = "Get operation status")
  @GetMapping(V1 + OPERATIONS_ID_STATUS)
  public OperationStatusDto getOperationStatus(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId) {
    return operationService.getOperationStatusDto(tenant, operationId);
  }

  @Operation(summary = "Search for operations status")
  @PostMapping(V1 + OPERATIONS_STATUS)
  public SliceResult<OperationStatusDto> searchOperationsStatus(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestBody OperationQuery operationQuery,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit) {

    PageRequest pageRequest = SearchUtils.createPageRequest(offset, limit);
    return operationService.searchOperationStatusDto(tenant, operationQuery, pageRequest);
  }
}
