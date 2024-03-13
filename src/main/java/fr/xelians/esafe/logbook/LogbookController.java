/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.logbook;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(LOGBOOK_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class LogbookController {

  private final LogbookService logbookService;
  private final SearchService searchService;
  private final AccessContractService accessContractService;

  // Logbook
  @Operation(summary = "Get operation indexed in the logbook")
  @GetMapping(V1 + LOGBOOK_OPERATIONS + "/{operationId}")
  public SearchResult<JsonNode> searchLogbookOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {

    return logbookService.searchLogbookOperation(tenant, operationId);
  }

  @Operation(summary = "Get operation indexed in the logbook")
  @GetMapping(V2 + LOGBOOK_OPERATIONS + "/{operationId}")
  public LogbookOperationDto getLogbookOperation(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @PathVariable Long operationId)
      throws IOException {

    return logbookService.getLogbookOperationDto(tenant, operationId);
  }

  @Operation(summary = "Search for operations indexed in the logbook with (GET)", hidden = true)
  @GetMapping(V1 + LOGBOOK_OPERATIONS_SEARCH)
  public SearchResult<JsonNode> searchLogbookOperationsWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {

    return logbookService.searchLogbookOperations(tenant, query);
  }

  @Operation(summary = "Search for operations indexed in the logbook")
  @PostMapping(V1 + LOGBOOK_OPERATIONS_SEARCH)
  public SearchResult<JsonNode> searchLogbookOperations(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query)
      throws IOException {

    return logbookService.searchLogbookOperations(tenant, query);
  }

  @Operation(summary = "Search for the given unit life cycles with (GET)", hidden = true)
  @GetMapping(V1 + LOGBOOK_UNIT_LIFECYCLES)
  public JsonNode getLogbookUnitLifecycles(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getUnitLifecycles(tenant, accessContractDb, unitId);
  }

  @Operation(summary = "Search for the given unit life cycles with")
  @GetMapping(V1 + LOGBOOK_OBJECT_LIFECYCLES)
  public JsonNode getLogbookObjectLifecycles(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long unitId)
      throws IOException {

    AccessContractDb accessContractDb = accessContractService.getEntity(tenant, accessContract);
    return searchService.getObjectLifecycles(tenant, accessContractDb, unitId);
  }
}
