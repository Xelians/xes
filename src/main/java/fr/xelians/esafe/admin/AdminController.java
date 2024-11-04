/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_DEPRECATED;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ROOT_ADMIN;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.ROLE_ARCHIVE_MANAGER;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

import fr.xelians.esafe.admin.service.AdminService;
import fr.xelians.esafe.admin.service.CoherencyService;
import fr.xelians.esafe.admin.service.IndexAdminService;
import fr.xelians.esafe.admin.service.OfferAdminService;
import fr.xelians.esafe.admin.task.AddOfferTask;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.dto.vitam.VitamExternalEventDto;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.security.resourceserver.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * The {@code AdminController} class provides REST API endpoints for administrative operations
 * related to managing search engine indices, checking system coherence, handling offers, and
 * managing external logbook operations.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class AdminController {

  private static final String CREATED = "{\"httpCode\":201}";

  private final ProcessingService processingService;
  private final CoherencyService coherencyService;
  private final OfferAdminService offerAdminService;
  private final IndexAdminService indexAdminService;
  private final LogbookService logbookService;
  private final AdminService adminService;

  @Operation(summary = "Get json batch report")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @GetMapping(value = V1 + BATCH_REPORT, produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getReport(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {
    InputStream objectStream = adminService.getReportStream(tenant, operationId);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  @Operation(summary = "Reset search engine index")
  @PreAuthorize("hasRole('" + ROLE_ROOT_ADMIN + "')")
  @PostMapping(V1 + RESET_SEARCH_ENGINE_INDEX)
  public ResponseEntity<Void> resetSearchEngineIndex() throws IOException {
    indexAdminService.resetAllIndex();
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Reset and update search engine index")
  @PreAuthorize("hasRole('" + ROLE_ROOT_ADMIN + "')")
  @PostMapping(V1 + REBUILD_SEARCH_ENGINE_INDEX)
  public ResponseEntity<Void> rebuildSearchEngineIndex() throws IOException {
    Long tenant = AuthContext.getTenant();
    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long operationId = indexAdminService.rebuildIndex(tenant, user, app);

    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operationId.toString());
    return ResponseEntity.accepted().headers(headers).build();
  }

  @Operation(summary = "Update (reindex) search engine index")
  @PreAuthorize("hasRole('" + ROLE_ROOT_ADMIN + "')")
  @PutMapping(V1 + UPDATE_SEARCH_ENGINE_INDEX)
  public ResponseEntity<Void> updateSearchEngineIndex() {
    Long tenant = AuthContext.getTenant();
    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long operationId = indexAdminService.updateIndex(tenant, user, app);

    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operationId.toString());
    return ResponseEntity.accepted().headers(headers).build();
  }

  @Operation(summary = "Check Coherence")
  @PreAuthorize("hasRole('" + ROLE_ROOT_ADMIN + "')")
  @PostMapping(V1 + CHECK_COHERENCE)
  public ResponseEntity<Void> checkCoherence(
      @PathVariable Optional<Integer> delay, @PathVariable Optional<Integer> duration) {

    Long tenant = AuthContext.getTenant();
    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    int del = delay.orElse(1);
    int dur = duration.orElse(1);
    Long operationId = coherencyService.checkCoherency(tenant, user, app, del, dur);

    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, operationId.toString());
    return ResponseEntity.accepted().headers(headers).build();
  }

  @Operation(summary = "Add offer to tenant")
  @PreAuthorize("hasRole('" + ROLE_ROOT_ADMIN + "')")
  @PutMapping(V1 + ADD_STORAGE_OFFER)
  public ResponseEntity<Void> addStorageOffer(@PathVariable String offer) {

    Long tenant = AuthContext.getTenant();
    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    OperationDb operation = offerAdminService.addOffer(tenant, offer, user, app);
    processingService.submit(new AddOfferTask(operation, offerAdminService));

    // FIX.
    // if rollback, task could be still active and copy in an orphean offer
    // if task fails, tenantdB could have an incomplete offer
    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, String.valueOf(operation.getId()));
    return ResponseEntity.accepted().headers(headers).build();
  }

  @Operation(summary = "Create an external logbook operation")
  @PreAuthorize("hasRole('" + ROLE_DEPRECATED + "')")
  @PostMapping(V1 + LOGBOOK_OPERATIONS)
  public ResponseEntity<String> createExternalLogbookOperation(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody VitamExternalEventDto vitamEventDto) {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = logbookService.createExternalLogbookOperation(tenant, user, app, vitamEventDto);

    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, id.toString());
    return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(CREATED);
  }
}
