/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(LOGBOOK_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class LogbookController {

  private final LogbookService logbookService;
  private final SearchService searchService;
  private final AccessContractService accessContractService;

  // Vitam Logbook
  @Operation(summary = "Get operation indexed in the logbook")
  @GetMapping(V1 + LOGBOOK_OPERATIONS + "/{operationId}")
  public VitamLogbookOperationDto getVitamLogbookOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {
    return logbookService.getVitamLogbookOperationDto(tenant, operationId);
  }

  @Operation(summary = "Search for one operation indexed in the logbook")
  @PostMapping(V1 + LOGBOOK_OPERATIONS + "/{operationId}")
  public SearchResult<VitamLogbookOperationDto> searchVitamLogbookOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {
    return logbookService.searchVitamLogbookOperationDto(tenant, operationId);
  }

  @Operation(summary = "Search for operations indexed in the logbook", hidden = true)
  @GetMapping(V1 + LOGBOOK_OPERATIONS_SEARCH)
  public SearchResult<VitamLogbookOperationDto> searchVitamLogbookOperationsWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return logbookService.searchVitamLogbookOperationDtos(tenant, query);
  }

  @Operation(summary = "Search for operations indexed in the logbook")
  @PostMapping(V1 + LOGBOOK_OPERATIONS_SEARCH)
  public SearchResult<VitamLogbookOperationDto> searchVitamLogbookOperations(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return logbookService.searchVitamLogbookOperationDtos(tenant, query);
  }

  // Standard Logbook
  @Operation(summary = "Get operation indexed in the logbook")
  @GetMapping(V2 + LOGBOOK_OPERATIONS + "/{operationId}")
  public LogbookOperationDto getLogbookOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {
    return logbookService.getLogbookOperationDto(tenant, operationId);
  }

  @Operation(summary = "Search for operations indexed in the logbook")
  @PostMapping(V2 + LOGBOOK_OPERATIONS_SEARCH)
  public SearchResult<LogbookOperationDto> searchLogbookOperations(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {
    return logbookService.searchLogbookOperationDtos(tenant, query);
  }

  // Lifecycles
  @Operation(summary = "Search for the given unit life cycles with (GET)", hidden = true)
  @GetMapping(V1 + LOGBOOK_UNIT_LIFECYCLES)
  public JsonNode getLogbookUnitLifecycles(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getUnitLifecycles(tenant, accessContractDb, unitId);
  }

  @Operation(summary = "Search for the given unit life cycles with")
  @GetMapping(V1 + LOGBOOK_OBJECT_LIFECYCLES)
  public JsonNode getLogbookObjectLifecycles(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getObjectLifecycles(tenant, accessContractDb, unitId);
  }
}
