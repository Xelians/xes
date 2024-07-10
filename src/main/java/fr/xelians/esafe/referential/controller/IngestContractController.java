/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.domain.SortDir;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.referential.domain.SortBy;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.dto.*;
import fr.xelians.esafe.referential.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
public class IngestContractController {

  private final IngestContractService ingestContractService;

  /*
   *   Ingest Contracts V1
   */
  @PostMapping(V1 + INGEST_CONTRACTS)
  public List<IngestContractDto> createIngestContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<IngestContractDto> ingestContracts) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return ingestContractService.create(tenant, userIdentifier, applicationId, ingestContracts);
  }

  // TODO Use PageResult !
  @GetMapping(V1 + INGEST_CONTRACTS + "/{identifier}")
  public IngestContractDto findIngestContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ingestContractService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + INGEST_CONTRACTS)
  public SearchResult<JsonNode> searchIngestContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ingestContractService.search(tenant, query);
  }

  @PostMapping(V2 + INGEST_CONTRACTS + "/search")
  public SearchResult<JsonNode> searchIngestContractsV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ingestContractService.search(tenant, query);
  }

  @PutMapping(V1 + INGEST_CONTRACTS + "/{identifier}")
  public IngestContractDto updateIngestContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody IngestContractDto ingestContract) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return ingestContractService.update(
        tenant, userIdentifier, applicationId, identifier, ingestContract);
  }

  /*
   *   Ingest Contracts V2
   */
  @GetMapping(V2 + INGEST_CONTRACTS + "/{identifier}")
  public IngestContractDto getIngestContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ingestContractService.getDto(tenant, identifier);
  }

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
}
