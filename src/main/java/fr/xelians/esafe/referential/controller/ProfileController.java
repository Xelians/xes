/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.controller;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;
import static org.springframework.http.MediaType.*;

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
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Validated
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
public class ProfileController {

  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  private final ProfileService profileService;

  /*
   *   Profiles V1
   */
  @PostMapping(V1 + PROFILES)
  public List<ProfileDto> createProfile(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<ProfileDto> profiles) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return profileService.create(tenant, userIdentifier, applicationId, profiles);
  }

  // TODO Use PageResult !
  @GetMapping(value = V1 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ProfileDto findProfileById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return profileService.getDto(tenant, identifier);
  }

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

  @GetMapping(V1 + PROFILES)
  public SearchResult<JsonNode> searchProfiles(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return profileService.search(tenant, query);
  }

  @PostMapping(V2 + PROFILES + "/search")
  public SearchResult<JsonNode> searchProfilesV2(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return profileService.search(tenant, query);
  }

  @PutMapping(value = V1 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ProfileDto updateProfile(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody ProfileDto profile) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return profileService.update(tenant, userIdentifier, applicationId, identifier, profile);
  }

  // TODO Add a standard VITAM endpoint
  @PutMapping(
      value = V1 + PROFILES + "/{identifier}/data",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public void updateProfileBinary(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam("file") MultipartFile multipartFile,
      @PathVariable String identifier)
      throws IOException {
    String user = AuthContext.getUserIdentifier();
    String app = AuthContext.getApplicationId();

    // Read until 512Kb and return an error beyond
    byte[] data = MultipartUtils.toBytes(multipartFile, 512_000);
    profileService.updateDataByIdentifier(tenant, user, app, identifier, data);
  }

  /*
   *   Profiles V2
   */
  @GetMapping(value = V2 + PROFILES + "/{identifier}", consumes = APPLICATION_JSON_VALUE)
  public ProfileDto getProfileById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return profileService.getDto(tenant, identifier);
  }

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
}
