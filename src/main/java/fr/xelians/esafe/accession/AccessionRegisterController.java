/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import fr.xelians.esafe.accession.dto.RegisterDetailsDto;
import fr.xelians.esafe.accession.dto.RegisterSummaryDto;
import fr.xelians.esafe.accession.service.AccessionRegisterService;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ADMIN_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class AccessionRegisterController {

  private final AccessionRegisterService accessionRegisterService;

  @Operation(summary = "Search for accession registers")
  @PostMapping(V1 + ACCESSION_REGISTER_SUMMARY)
  public SearchResult<RegisterSummaryDto> searchAccessionRegisterSummary(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {

    return accessionRegisterService.searchSummary(tenant, query);
  }

  @Operation(summary = "Search for detailed accession registers")
  @PostMapping(V1 + ACCESSION_REGISTER_DETAILS)
  public SearchResult<RegisterDetailsDto> searchAccessionRegisterDetails(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {

    return accessionRegisterService.searchDetails(tenant, query);
  }
}
