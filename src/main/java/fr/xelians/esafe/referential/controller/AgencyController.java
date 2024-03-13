/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
import fr.xelians.esafe.common.exception.functional.BadRequestException;
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
public class AgencyController {

  private final AgencyService agencyService;

  /*
   *   Agencies V1
   */
  @PostMapping(value = V1 + AGENCIES, consumes = TEXT_PLAIN_VALUE)
  public void createAgencyCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody String csv) {
    if (Utils.isNotHtmlSafe(csv)) {
      throw new BadRequestException("Agency creation failed", "Agency Csv contains html");
    }
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    agencyService.createAgencyCsv(tenant, userIdentifier, applicationId, csv);
  }

  @GetMapping(V1 + AGENCIES + "/{identifier}/csv")
  public String findAgencyByIdentifierCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getAgencyCsv(tenant, identifier);
  }

  @GetMapping(V1 + AGENCIES + "/csv")
  public String findAgenciesCsv(@RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant) {
    return agencyService.getAgenciesCsv(tenant);
  }

  // TODO Use PageResult !
  @GetMapping(V1 + AGENCIES + "/{identifier}")
  public AgencyDto findAgencyByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + AGENCIES)
  public SearchResult<JsonNode> searchAgencies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return agencyService.search(tenant, query);
  }

  /*
   *   Agencies V2
   */
  @PostMapping(V2 + AGENCIES)
  public List<AgencyDto> createAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<AgencyDto> agencies) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return agencyService.create(tenant, userIdentifier, applicationId, agencies);
  }

  @GetMapping(V2 + AGENCIES + "/{identifier}")
  public AgencyDto getAgencyByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return agencyService.getDto(tenant, identifier);
  }

  @GetMapping(V2 + AGENCIES)
  public PageResult<AgencyDto> findAgencies(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return agencyService.getDtos(tenant, name, status, pageRequest);
  }

  @PutMapping(V2 + AGENCIES + "/{identifier}")
  public AgencyDto updateAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody AgencyDto agency) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return agencyService.update(tenant, userIdentifier, applicationId, identifier, agency);
  }

  @DeleteMapping(V2 + AGENCIES + "/{identifier}")
  public void deleteAgency(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    agencyService.delete(identifier, tenant);
  }
}
