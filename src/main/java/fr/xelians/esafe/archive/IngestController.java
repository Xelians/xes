/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;
import static org.springframework.http.MediaType.*;

import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.archive.service.IngestService;
import fr.xelians.esafe.authentication.domain.AuthContext;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class IngestController {

  private final IngestService ingestService;

  @Operation(summary = "Ingest Vitam archive")
  @PostMapping(value = INGEST_EXTERNAL + V1 + INGESTS, consumes = APPLICATION_OCTET_STREAM_VALUE)
  // @Secured("ROLE_INGEST")
  public ResponseEntity<Void> ingest(
      final HttpServletRequest request,
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_CONTEXT_ID) ContextId contextId)
      throws IOException {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();

    Long id = ingestService.ingestStream(tenant, contextId, request.getInputStream(), user, app);
    return accepted(id);
  }

  @Operation(summary = "Ingest archive")
  @PostMapping(value = INGEST_EXTERNAL + V2 + INGESTS, consumes = MULTIPART_FORM_DATA_VALUE)
  // @Secured("ROLE_INGEST")
  public ResponseEntity<Void> ingest(
      @RequestParam(X_FILE) MultipartFile multipartFile,
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_CONTEXT_ID) ContextId contextId,
      @RequestHeader(value = ACCEPT, required = false) @Size(max = 1024) String accept)
      throws IOException {

    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();
    Long id = ingestService.ingestMultipartFile(tenant, contextId, multipartFile, user, app);
    return accepted(id);
  }

  private ResponseEntity<Void> accepted(Long id) {
    HttpHeaders headers = new HttpHeaders();
    headers.add(X_REQUEST_ID, id.toString());
    return ResponseEntity.accepted().headers(headers).build();
  }

  // Manifest
  @Operation(summary = "Get Xml Manifest from Operation Id")
  @GetMapping(value = INGEST_EXTERNAL + V1 + MANIFESTS, produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getManifest(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {

    InputStream objectStream = ingestService.getManifestStream(tenant, operationId);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  // ATR
  @Operation(summary = "Get Xml ATR from Operation Id")
  @GetMapping(value = INGEST_EXTERNAL + V1 + ATR_XML, produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getXmlAtr(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException, XMLStreamException {

    InputStream objectStream = ingestService.getXmlAtrStream(tenant, operationId);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  @Operation(summary = "Get Json ATR from Operation Id")
  @GetMapping(value = INGEST_EXTERNAL + V1 + ATR_JSON, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<InputStreamResource> getJsonAtr(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {

    InputStream objectStream = ingestService.getJsonAtrStream(tenant, operationId);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(bodyStream);
  }
}
