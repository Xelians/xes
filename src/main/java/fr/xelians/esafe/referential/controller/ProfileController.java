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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * The {@code ProfileController} class handles the REST API endpoints for managing user profiles. It
 * provides operations to create, read, update, delete, and search profiles.
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@RequiredArgsConstructor
public class ProfileController {

  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  private final ProfileService profileService;

  @Operation(summary = "Create a new profile")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PostMapping(V1 + PROFILES)
  public ResponseEntity<List<ProfileDto>> createProfile(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<ProfileDto> profiles) {

    OperationDb operation = createOperation(OperationType.CREATE_PROFILE, tenant);
    List<ProfileDto> dtos = profileService.createProfiles(operation, tenant, profiles);
    HttpHeaders headers = createHeaders(operation);
    URI location = URI.create(ADMIN_EXTERNAL + V1 + PROFILES);
    return ResponseEntity.created(location).headers(headers).body(dtos);
  }

  // TODO Use PageResult !
  @Operation(summary = "Find a profile by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V1 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ProfileDto findProfileById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return profileService.getDto(tenant, identifier);
  }

  @Operation(summary = "Get profile data by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(
      value = V1 + PROFILES + "/{identifier}/data",
      produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Resource> getDataProfileById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    ByteContent content = profileService.getProfileData(tenant, identifier);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(CONTENT_DISPOSITION, "attachment; filename=\"" + content.name() + "\"")
        .body(new ByteArrayResource(content.bytes()));
  }

  @Operation(summary = "Search for profiles")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V1 + PROFILES)
  public SearchResult<ProfileDto> searchProfiles(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return profileService.search(tenant, query);
  }

  @Operation(summary = "Search for profiles using V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @PostMapping(V2 + PROFILES + "/search")
  public SearchResult<ProfileDto> searchProfilesV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return profileService.search(tenant, query);
  }

  @Operation(summary = "Update a profile by identifier")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(value = V1 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ResponseEntity<ProfileDto> updateProfile(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody ProfileDto profile) {

    OperationDb operation = createOperation(OperationType.UPDATE_PROFILE, tenant);
    ProfileDto dto = profileService.updateProfile(operation, tenant, identifier, profile);
    HttpHeaders headers = createHeaders(operation);
    return ResponseEntity.ok().headers(headers).body(dto);
  }

  @Operation(summary = "Update profile data using octet stream")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(
      value = V1 + PROFILES + "/{identifier}/data",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Void> updateProfileBinary(
      final HttpServletRequest request,
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier)
      throws IOException {

    // Read until 512Kb and return an error beyond
    byte[] data = NioUtils.toBytes(request.getInputStream(), 512_000);
    return doUpdate(tenant, identifier, data);
  }

  private ResponseEntity<Void> doUpdate(Long tenant, String identifier, byte[] data)
      throws IOException {
    OperationDb operation = createOperation(OperationType.UPDATE_PROFILE, tenant);
    profileService.updateDataByIdentifier(operation, tenant, identifier, data);
    HttpHeaders headers = createHeaders(operation);
    ResponseEntity.ok().headers(headers);
    return new ResponseEntity<>(headers, HttpStatus.OK);
  }

  @Operation(summary = "Update profile data using multipart form data")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @PutMapping(
      value = V2 + PROFILES + "/{identifier}/data",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> updateProfileBinaryV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam("file") MultipartFile multipartFile,
      @PathVariable String identifier)
      throws IOException {

    // Read until 512Kb and return an error beyond
    byte[] data = NioUtils.toBytes(multipartFile, 512_000);
    return doUpdate(tenant, identifier, data);
  }

  @Operation(summary = "Get a profile by identifier using V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(value = V2 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ProfileDto getProfileById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return profileService.getDto(tenant, identifier);
  }

  @Operation(summary = "Find profiles using V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_READER + "')")
  @GetMapping(V2 + PROFILES)
  public PageResult<ProfileDto> findProfiles(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return profileService.getDtos(tenant, name, status, pageRequest);
  }

  @Operation(summary = "Delete a profile by identifier using V2")
  @PreAuthorize("hasRole('" + ROLE_ARCHIVE_MANAGER + "')")
  @DeleteMapping(V2 + PROFILES + "/{identifier}")
  public ResponseEntity<Void> deleteProfile(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {

    OperationDb operation = createOperation(OperationType.DELETE_AGENCY, tenant);
    profileService.deleteProfile(operation, tenant, identifier);
    HttpHeaders headers = createHeaders(operation);
    ResponseEntity.ok().headers(headers);
    return new ResponseEntity<>(headers, HttpStatus.OK);
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
