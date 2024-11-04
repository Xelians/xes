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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The {@code RuleController} class provides REST APIs to manage rules in the system, including
 * creating, fetching, updating, deleting rules, and generating reports.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class RuleController {

  private final RuleService ruleService;

  @Operation(summary = "Create rules from CSV and return the created rules")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + RULES, consumes = TEXT_PLAIN_VALUE)
  public ResponseEntity<List<RuleDto>> createRuleCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody String csv) {

    validateCsvInput(csv);

    OperationDb operation = createOperation(OperationType.CREATE_RULE, tenant);
    List<RuleDto> ruleDtos = ruleService.createCsvRules(operation, tenant, csv);

    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operation.getId().toString());
    URI location = URI.create(ADMIN_EXTERNAL + V1 + RULES + "/csv");
    // It differs from Vitam by returning the created rules in the body
    return ResponseEntity.created(location).headers(headers).body(ruleDtos);
  }

  @Operation(summary = "Find rule by identifier as CSV")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + RULES + "/{identifier}/csv")
  public String findRuleByIdentifierCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getRulesByIdentifier(identifier, tenant);
  }

  @Operation(summary = "Find all rules as CSV")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + RULES + "/csv")
  public String findRulesCsv(@RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant) {
    return ruleService.getCsvRules(tenant);
  }

  @Operation(summary = "Find rule by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + RULES + "/{identifier}")
  public RuleDto findRuleByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getDto(tenant, identifier);
  }

  @Operation(summary = "Search rules")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + RULES)
  public SearchResult<RuleDto> searchRules(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ruleService.search(tenant, query);
  }

  @Operation(summary = "Get rule report")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @GetMapping(value = V1 + RULES_REPORT, produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getReport(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {
    InputStream objectStream = ruleService.getRuleReport(tenant, operationId);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  // V2

  @Operation(summary = "Create rules")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(V2 + RULES)
  public ResponseEntity<List<RuleDto>> createRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<RuleDto> rules) {

    OperationDb operation = createOperation(OperationType.CREATE_RULE, tenant);
    List<RuleDto> ruleDtos = ruleService.createRules(operation, tenant, rules);

    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V2 + RULES);
    return ResponseEntity.created(location).headers(headers).body(ruleDtos);
  }

  @Operation(summary = "Get rule by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + RULES + "/{identifier}")
  public RuleDto getRuleByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getDto(tenant, identifier);
  }

  @Operation(summary = "Find all rules with pagination")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + RULES)
  public PageResult<RuleDto> findRules(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return ruleService.getDtos(tenant, name, status, pageRequest);
  }

  @Operation(summary = "Update rule")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(V2 + RULES + "/{identifier}")
  public ResponseEntity<RuleDto> updateRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody RuleDto rule) {
    OperationDb operation = createOperation(OperationType.UPDATE_RULE, tenant);
    RuleDto ruleDto = ruleService.updateRule(operation, tenant, identifier, rule);

    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(ruleDto);
  }

  @Operation(summary = "Delete rule")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @DeleteMapping(V2 + RULES + "/{identifier}")
  public ResponseEntity<Void> deleteRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier)
      throws IOException {

    OperationDb operation = createOperation(OperationType.DELETE_RULE, tenant);
    ruleService.deleteRule(operation, tenant, identifier);

    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).build();
  }

  @Operation(summary = "Search rules")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V2 + RULES + "/search")
  public SearchResult<RuleDto> searchRulesV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ruleService.search(tenant, query);
  }

  private void validateCsvInput(String csv) {
    if (csv.length() > 1_000_000) {
      throw new BadRequestException("Rule creation failed", "Rule Csv is too big");
    }
    if (Utils.isNotHtmlSafe(csv)) {
      throw new BadRequestException("Rule creation failed", "Rule Csv contains html");
    }
  }

  private OperationDb createOperation(OperationType type, Long tenant) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return OperationFactory.createReferentialOp(type, tenant, userIdentifier, applicationId);
  }

  private HttpHeaders createHeaders(OperationDb operation) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operation.getId().toString());
    return headers;
  }
}
