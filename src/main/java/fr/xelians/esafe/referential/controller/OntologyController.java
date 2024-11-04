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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The OntologyController is a REST controller that handles requests related to ontologies. It
 * provides methods to create, retrieve, update, search, and delete ontologies. This controller
 * requires specific roles for certain actions, ensuring that only authorized users can perform
 * operations on ontologies.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class OntologyController {

  private final OntologyService ontologyService;

  @Operation(summary = "Create a new ontology")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(V1 + ONTOLOGIES)
  public ResponseEntity<List<OntologyDto>> createOntology(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<OntologyDto> ontologyDtos) {

    OperationDb operation = createOperation(OperationType.CREATE_ONTOLOGY, tenant);
    List<OntologyDto> dtos = ontologyService.createOntology(operation, tenant, ontologyDtos);
    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + ONTOLOGIES);
    return ResponseEntity.created(location).headers(headers).body(dtos);
  }

  // TODO Use PageResult !
  @Operation(summary = "Find an ontology by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + ONTOLOGIES + "/{identifier}")
  public OntologyDto findOntologyById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ontologyService.getDto(tenant, identifier);
  }

  @Operation(summary = "Search ontologies")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + ONTOLOGIES)
  public SearchResult<OntologyDto> searchOntologies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ontologyService.search(tenant, query);
  }

  @Operation(summary = "Update an ontology")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(V1 + ONTOLOGIES + "/{identifier}")
  public ResponseEntity<OntologyDto> updateOntology(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody OntologyDto ontologyDto) {

    OperationDb operation = createOperation(OperationType.UPDATE_ONTOLOGY, tenant);
    OntologyDto dto = ontologyService.updateOntology(operation, tenant, identifier, ontologyDto);
    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(dto);
  }

  @Operation(summary = "Delete an ontology")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @DeleteMapping(V1 + ONTOLOGIES + "/{identifier}")
  public ResponseEntity<Void> deleteOntology(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {

    OperationDb operation = createOperation(OperationType.DELETE_ONTOLOGY, tenant);
    ontologyService.deleteOntology(operation, tenant, identifier);
    HttpHeaders headers = createHeaders(operation);
    return new ResponseEntity<>(headers, HttpStatus.OK);
  }

  @Operation(summary = "Get an ontology by identifier V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + ONTOLOGIES + "/{identifier}")
  public OntologyDto getOntologyById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ontologyService.getDto(tenant, identifier);
  }

  @Operation(summary = "Find ontologies")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + ONTOLOGIES)
  public PageResult<OntologyDto> findOntologies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return ontologyService.getDtos(tenant, name, status, pageRequest);
  }

  @Operation(summary = "Search ontologies V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V2 + ONTOLOGIES + "/search")
  public SearchResult<OntologyDto> searchOntologiesV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ontologyService.search(tenant, query);
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
