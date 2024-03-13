/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
public class AccessContractController {

  private final AccessContractService accessContractService;

  /*
   *   Access Contracts V1
   */
  @PostMapping(V1 + ACCESS_CONTRACTS)
  public List<AccessContractDto> createAccessContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<AccessContractDto> accessContracts) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return accessContractService.create(tenant, userIdentifier, applicationId, accessContracts);
  }

  // TODO Use PageResult !
  @GetMapping(V1 + ACCESS_CONTRACTS + "/{identifier}")
  public AccessContractDto findAccessContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return accessContractService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + ACCESS_CONTRACTS)
  public SearchResult<JsonNode> searchAccessContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return accessContractService.search(tenant, query);
  }

  @PutMapping(V1 + ACCESS_CONTRACTS + "/{identifier}")
  public AccessContractDto updateAccessContract(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody AccessContractDto accessContract) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return accessContractService.update(
        tenant, userIdentifier, applicationId, identifier, accessContract);
  }

  /*
   *   Access Contracts V2
   */
  @GetMapping(V2 + ACCESS_CONTRACTS + "/{identifier}")
  public AccessContractDto getAccessContractById(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return accessContractService.getDto(tenant, identifier);
  }

  @GetMapping(V2 + ACCESS_CONTRACTS)
  public PageResult<AccessContractDto> findAccessContracts(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {
    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return accessContractService.getDtos(tenant, name, status, pageRequest);
  }
}
