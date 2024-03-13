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
public class RuleController {

  private final RuleService ruleService;

  /*
   *   Rules V1
   */
  @PostMapping(value = V1 + RULES, consumes = TEXT_PLAIN_VALUE)
  public void createRuleCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody String csv) {
    if (Utils.isNotHtmlSafe(csv)) {
      throw new BadRequestException("Rule creation failed", "Rule Csv contains html");
    }
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    ruleService.createRuleCsv(tenant, userIdentifier, applicationId, csv);
  }

  @GetMapping(V1 + RULES + "/{identifier}/csv")
  public String findRuleByIdentifierCsv(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getRulesByIdentifier(identifier, tenant);
  }

  @GetMapping(V1 + RULES + "/csv")
  public String findRulesCsv(@RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant) {
    return ruleService.getRulesCsv(tenant);
  }

  // TODO Use PageResult !
  @GetMapping(V1 + RULES + "/{identifier}")
  public RuleDto findRuleByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getDto(tenant, identifier);
  }

  @GetMapping(V1 + RULES)
  public SearchResult<JsonNode> searchRules(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @RequestBody SearchQuery query) {
    return ruleService.search(tenant, query);
  }

  /*
   *   Rules V2
   */
  @PostMapping(V2 + RULES)
  public List<RuleDto> createRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @Valid @RequestBody List<RuleDto> rules) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return ruleService.create(tenant, userIdentifier, applicationId, rules);
  }

  @GetMapping(V2 + RULES + "/{identifier}")
  public RuleDto getRuleByIdentifier(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    return ruleService.getDto(tenant, identifier);
  }

  @GetMapping(V2 + RULES)
  public PageResult<RuleDto> findRules(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) Status status,
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "identifier") SortBy sortby,
      @RequestParam(defaultValue = "asc") SortDir sortdir) {

    PageRequest pageRequest =
        SearchUtils.createPageRequest(offset, limit, sortby.toString(), sortdir);
    return ruleService.getDtos(tenant, name, status, pageRequest);
  }

  @PutMapping(V2 + RULES + "/{identifier}")
  public RuleDto updateRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant,
      @PathVariable String identifier,
      @Valid @RequestBody RuleDto rule) {
    String userIdentifier = AuthContext.getUserIdentifier();
    String applicationId = AuthContext.getApplicationId();
    return ruleService.update(tenant, userIdentifier, applicationId, identifier, rule);
  }

  @DeleteMapping(V2 + RULES + "/{identifier}")
  public void deleteRule(
      @RequestHeader(Header.X_TENANT_ID) @Min(0) Long tenant, @PathVariable String identifier) {
    ruleService.delete(identifier, tenant);
  }
}
