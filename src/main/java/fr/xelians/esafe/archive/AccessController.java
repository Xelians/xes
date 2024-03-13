/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.*;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ARCHIVE_READER;
import static org.springframework.http.MediaType.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.converter.ObjectConverter;
import fr.xelians.esafe.archive.domain.converter.UnitConverter;
import fr.xelians.esafe.archive.domain.search.elimination.EliminationQuery;
import fr.xelians.esafe.archive.domain.search.export.ExportQuery;
import fr.xelians.esafe.archive.domain.search.probativevalue.ProbativeValueQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.domain.search.update.UpdateQuery;
import fr.xelians.esafe.archive.domain.search.updaterule.UpdateRuleQuery;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.archive.service.*;
import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.common.utils.StreamContent;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ACCESS_EXTERNAL)
@Secured(ROLE_ADMIN)
@RequiredArgsConstructor
@Validated
public class AccessController {

  private static final String ACCEPTED = "{\"httpCode\":202}";
  public static final String CONTENT_DISPOSITION = "Content-Disposition";

  private final AccessContractService accessContractService;
  private final SearchService searchService;
  private final IngestService ingestService;
  private final UpdateService updateService;
  private final EliminationService eliminationService;
  private final ReclassificationService reclassificationService;
  private final ExportService exportService;
  private final UpdateRulesService updateRulesService;
  private final ProbativeValueService probativeValueService;

  // Get One Archive Unit
  @Operation(summary = "Get one archive unit")
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
  @GetMapping(value = V1 + UNITS)
  public SearchResult<JsonNode> searchArchiveUnitsWithGet(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units (" + X_HTTP_METHOD_OVERRIDE + ")", hidden = true)
  @PostMapping(value = V1 + UNITS, headers = X_HTTP_METHOD_OVERRIDE)
  public SearchResult<JsonNode> searchArchiveUnitsWithOverride(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units")
  @PostMapping(value = V1 + UNITS + "/search")
  public SearchResult<JsonNode> searchArchiveUnits(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @RequestBody SearchQuery query)
      throws IOException {

    return searchUnits(tenant, accessContract, query);
  }

  @Operation(summary = "Search for archive units with inherited rules (GET)", hidden = true)
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
  @Operation(summary = "Get DIP from Operation Id")
  @GetMapping(value = V1 + EXPORT + "/{operationId}/dip", produces = APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<InputStreamResource> getDip(
      @RequestHeader(X_TENANT_ID) @Min(0) Long tenant,
      @RequestHeader(X_ACCESS_CONTRACT_ID) @Size(min = 1, max = 1024) String accessContract,
      @PathVariable Long operationId)
      throws IOException {

    InputStream objectStream = ingestService.getDipStream(tenant, operationId, accessContract);
    InputStreamResource bodyStream = new InputStreamResource(objectStream);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bodyStream);
  }

  @Secured(value = ROLE_ARCHIVE_READER)
  @Operation(summary = "Export selected archive units as DIP")
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

  // Check Archive Unit probative value
  @Operation(summary = "Export probative value report for selected archive units")
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
    return ResponseEntity.accepted().headers(headers).body(ACCEPTED);
  }
}
