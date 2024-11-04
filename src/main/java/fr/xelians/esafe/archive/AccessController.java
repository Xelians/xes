/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_MANAGER;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_READER;
import static org.springframework.http.MediaType.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.converter.ObjectConverter;
import fr.xelians.esafe.archive.domain.converter.UnitConverter;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationQuery;
import fr.xelians.esafe.archive.domain.search.export.ExportQuery;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.domain.search.transfer.TransferQuery;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRuleQuery;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.archive.service.*;
import fr.xelians.esafe.common.utils.StreamContent;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * The {@code AccessController} class handles HTTP requests related to archive unit access and
 * management. This class provides endpoints for retrieving, searching, updating, and managing
 * archive units, as well as operations related to binary objects, export, transfer, elimination,
 * and probative value checks.
 *
 * <p>It supports several versions (V1, V2) of some API endpoints for backward compatibility.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@RestController
@RequestMapping(ACCESS_EXTERNAL)
@RequiredArgsConstructor
@Validated
public class AccessController {

  // Warning. The number of hits may not be what the VITAM client expects
  private static final String VITAM_RESPONSE_BODY =
      "{\"httpCode\":202,\"$hits\":{\"total\":1,\"offset\":0,\"limit\":0,\"size\":1}}";
  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  private final AccessContractService accessContractService;
  private final SearchService searchService;
  private final IngestService ingestService;
  private final UpdateService updateService;
  private final EliminationService eliminationService;
  private final ReclassificationService reclassificationService;
  private final ExportService exportService;
  private final TransferService transferService;
  private final TransferReplyService transferReplyService;
  private final UpdateRulesService updateRulesService;
  private final ProbativeValueService probativeValueService;

  // Get One Archive Unit
  @Operation(summary = "Get one archive unit")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + UNITS + "/{unitId}")
  public SearchResult<JsonNode> searchArchiveUnit(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchUnit(tenant, accessContractDb, unitId);
  }

  @Operation(summary = "Get one archive unit")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V2 + UNITS + "/{unitId}")
  public JsonNode getArchiveUnit(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getUnit(tenant, accessContractDb, unitId);
  }

  // Search Archive Unit
  @Operation(summary = "Search for archive units (GET)", hidden = true)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + UNITS)
  public SearchResult<JsonNode> searchArchiveUnitsWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units (" + X_HTTP_METHOD_OVERRIDE + ")", hidden = true)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(value = V1 + UNITS, headers = X_HTTP_METHOD_OVERRIDE)
  public SearchResult<JsonNode> searchArchiveUnitsWithOverride(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(value = V1 + UNITS + "/search")
  public SearchResult<JsonNode> searchArchiveUnits(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units with inherited rules (GET)", hidden = true)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + UNITS_WITH_INHERITED_RULES)
  public SearchResult<JsonNode> searchArchiveUnitsInheritedRulesWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchWithInheritedRules(tenant, accessContractDb, query);
  }

  @Operation(summary = "Search for archive units with inherited rules")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(value = V1 + UNITS_WITH_INHERITED_RULES, headers = X_HTTP_METHOD_OVERRIDE)
  public SearchResult<JsonNode> searchArchiveUnitsWithInheritedRules(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchWithInheritedRules(tenant, accessContractDb, query);
  }

  private SearchResult<JsonNode> searchUnits(Long tenant, String accessContract, SearchQuery query)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchUnits(tenant, accessContractDb, query, UnitConverter.INSTANCE);
  }

  @Operation(summary = "Search for archive units as stream")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + UNITS_STREAM)
  public Stream<JsonNode> streamArchiveUnits(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    // Spring outputs the stream without buffering in memory. See:
    // https://stackoverflow.com/questions/66348194/does-responseentitystreammyobject-stores-everything-in-the-memory
    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchStream(tenant, accessContractDb, query, UnitConverter.INSTANCE);
  }

  // Get Object Metadata
  @Operation(summary = "Get object metadata from archive unit id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(
      value = V1 + UNITS + "/{unitId}/objects",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public SearchResult<JsonNode> getObjectMetadata(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getObjectMetadataByUnitId(tenant, accessContractDb, unitId);
  }

  // Search Object Metadata
  @Operation(summary = "Search for object metadata")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(
      value = V1 + OBJECTS,
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public SearchResult<JsonNode> searchObjectMetadata(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchUnits(tenant, accessContractDb, query, ObjectConverter.INSTANCE);
  }

  @Operation(summary = "Search for object metadata v2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(
      value = V2 + OBJECTS,
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public SearchResult<JsonNode> searchObjectMetadataV2(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.searchUnits(tenant, accessContractDb, query, ObjectConverter.INSTANCE);
  }

  // Get Binary Objects
  @Operation(summary = "Get binary object from archive unit id", hidden = true)
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + UNITS + "/{unitId}/objects")
  public ResponseEntity<InputStreamResource> getBinaryObject(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestHeader(X_QUALIFIER) BinaryQualifier qualifier,
      @RequestHeader(value = X_VERSION, required = false) @Min(0) Integer version,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    BinaryVersion binaryVersion = new BinaryVersion(qualifier, version);
    StreamContent content =
        searchService.getBinaryObjectByUnitId(tenant, accessContractDb, unitId, binaryVersion);

    InputStreamResource bodyStream = new InputStreamResource(content.inputStream());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.mimetype()))
        .header(CONTENT_DISPOSITION, getContentDispositionValue(content.name()))
        .body(bodyStream);
  }

  @Operation(summary = "Get binary object V2 from archive unit id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V2 + UNITS + "/{unitId}/objects")
  public ResponseEntity<InputStreamResource> getBinaryObjectV2(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestHeader(X_QUALIFIER) BinaryQualifier qualifier,
      @RequestHeader(value = X_VERSION, required = false) @Min(0) Integer version,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    BinaryVersion binaryVersion = new BinaryVersion(qualifier, version);
    StreamContent content =
        searchService.getBinaryObjectByUnitId(tenant, accessContractDb, unitId, binaryVersion);

    InputStreamResource bodyStream = new InputStreamResource(content.inputStream());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.mimetype()))
        .header(CONTENT_DISPOSITION, getContentDispositionValue(content.name()))
        .body(bodyStream);
  }

  @Operation(summary = "Get binary object from binary id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + OBJECTS + "/{binaryId}")
  public ResponseEntity<InputStreamResource> getBinaryObject(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long binaryId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    StreamContent content =
        searchService.getBinaryObjectByBinaryId(tenant, accessContractDb, binaryId);

    InputStreamResource bodyStream = new InputStreamResource(content.inputStream());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.mimetype()))
        .header(CONTENT_DISPOSITION, getContentDispositionValue(content.name()))
        .body(bodyStream);
  }

  private static String getContentDispositionValue(String filename) {
    return "attachment; filename=\"" + filename + "\"";
  }

  // Modify Archive Unit
  @Operation(summary = "Update selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + UNITS, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> update(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody UpdateQuery updateQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = updateService.update(tenant, accessContract, updateQuery, user, app);
    return accepted(id);
  }

  @Operation(summary = "Update rules for selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + UNITS_RULES, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updateRule(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody UpdateRuleQuery updateRuleQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = updateRulesService.updateRules(tenant, accessContract, updateRuleQuery, user, app);
    return accepted(id);
  }

  @Operation(summary = "Reclassify selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + RECLASSIFICATION, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> reclassify(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody UpdateQuery updateQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = reclassificationService.reclassify(tenant, accessContract, updateQuery, user, app);
    return accepted(id);
  }

  @Operation(summary = "Eliminate selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + ELIMINATION_ACTION, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> eliminate(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody EliminationQuery eliminationQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = eliminationService.eliminate(tenant, accessContract, eliminationQuery, user, app);
    return accepted(id);
  }

  // Get & Export DIP
  @Operation(summary = "Get exported DIP from Operation Id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @GetMapping(value = V1 + EXPORT + "/{operationId}/dip", produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getExportedDip(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long operationId)
      throws IOException {

    InputStream objectStream = exportService.getDipStream(tenant, operationId, accessContract);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  @Operation(summary = "Export selected archive units as DIP")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + EXPORT, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> exportDip(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody ExportQuery exportQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = exportService.export(tenant, accessContract, exportQuery, user, app);
    return accepted(id);
  }

  // Transfer SIP
  @Operation(summary = "Get transferred SIP from Operation Id")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @GetMapping(
      value = V1 + TRANSFER + "/{operationId}/sip",
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getTransferredSip(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long operationId)
      throws IOException {

    InputStream objectStream = transferService.getDipStream(tenant, operationId, accessContract);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  @Operation(summary = "Transfer selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + TRANSFER, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> transferSip(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody TransferQuery transferQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = transferService.transfer(tenant, accessContract, transferQuery, user, app);
    return accepted(id);
  }

  @Operation(summary = "Eliminate transferred archives from ATR")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + TRANSFER_REPLY, consumes = APPLICATION_XML_VALUE)
  public ResponseEntity<String> transferReply(
      final HttpServletRequest request,
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract)
      throws IOException {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id =
        transferReplyService.transferReply(
            tenant, accessContract, request.getInputStream(), user, app);
    return accepted(id);
  }

  // Check Archive Unit probative value
  @Operation(summary = "Export probative value report for selected archive units")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(value = V1 + PROBATIVE_VALUE_EXPORT, consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<String> probativeValue(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody ProbativeValueQuery probativeValueQuery) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id =
        probativeValueService.probativeValue(
            tenant, accessContract, probativeValueQuery, user, app);
    return accepted(id);
  }

  private ResponseEntity<String> accepted(Long id) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, id.toString());
    return ResponseEntity.accepted().headers(headers).body(VITAM_RESPONSE_BODY);
  }
}
