/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitParser;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.referential.domain.RuleType;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.eql.RootQuery;
import io.jsonwebtoken.lang.Assert;
import java.util.*;

public class UpdateRuleParser extends ArchiveUnitParser {

  public UpdateRuleParser(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    super(tenant, accessContractDb, ontologyMapper);
  }

  public static UpdateRuleParser create(
      Long tenant, AccessContractDb accessContractDb, OntologyMapper ontologyMapper) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContractDb, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(ontologyMapper, ONTOLOGY_MAPPER_MUST_BE_NOT_NULL);

    return new UpdateRuleParser(tenant, accessContractDb, ontologyMapper);
  }

  public UpdateRulesRequest createRequest(UpdateRuleQuery updateRuleQuery) {
    return new UpdateRulesRequest(
        doCreateUpdateRulesRequest(updateRuleQuery.searchQuery()),
        doCreateRuleActions(updateRuleQuery.ruleActions()));
  }

  private RuleLists doCreateRuleActions(RuleActions ruleActions) {
    List<RuleTypeName> deleteRules = getDeleteRules(ruleActions.delete());
    Map<RuleType, UpdateRules> updates = getUpdateRules(ruleActions.update());
    List<AbstractRules> addRules = getAddRules(ruleActions.add());
    return new RuleLists(deleteRules, updates, addRules);
  }

  public SearchRequest doCreateUpdateRulesRequest(SearchQuery searchQuery) {
    if (isEmpty(searchQuery.queryNode())) {
      throw new BadRequestException(CREATION_FAILED, QUERY_IS_EMPTY_OR_NOT_DEFINED);
    }

    if (accessContractDb.getStatus() == Status.INACTIVE) {
      throw new BadRequestException(
          CREATION_FAILED,
          String.format(ACCESS_CONTRACT_IS_INACTIVE, accessContractDb.getIdentifier()));
    }

    // The context of this query
    SearchContext searchContext = new SearchContext(searchQuery.type());

    // Obtain root units
    List<Long> roots =
        searchQuery.roots() == null
            ? Collections.emptyList()
            : searchQuery.roots().stream().filter(id -> id >= 0).toList();

    // Obtain root query
    RootQuery rootQuery = createRootQuery(searchContext, searchQuery.queryNode());
    int depth = Math.max(0, rootQuery.depth());

    // Create filter queries
    List<Query> filterQueries = createFilterQueries(searchContext, roots, depth);

    // Create sort options
    List<SortOptions> sortOptions = createSortOptions(searchContext, searchQuery.filterNode());

    // Create from & size
    int[] limits = createLimits(searchQuery.filterNode());

    // Create search request
    return SearchRequest.of(
        s ->
            s.index(searchable.getAlias())
                .query(b -> b.bool(m -> m.must(rootQuery.query()).filter(filterQueries)))
                .from(limits[FROM])
                .size(limits[SIZE])
                .sort(sortOptions)
                .source(createSourceFilter(Collections.emptyList())));
  }

  //  "delete": [ { "HoldRule": { "Rules": [ { "Rule": "HOL-00001" } ] } },
  //              { "ReuseRule": { "Rules": [ { "Rule": "REU-00001" } ] } } ]
  private List<RuleTypeName> getDeleteRules(List<JsonNode> rules) {
    Set<RuleTypeName> deleteRules = new HashSet<>();
    if (rules != null) {
      for (JsonNode rule : rules) {
        if (!rule.isObject()) {
          throw new BadRequestException(
              CREATION_FAILED, String.format("Delete rule '%s' have to be an object", rule));
        }
        rule.fields().forEachRemaining(e -> ruleToDelete(e, deleteRules));
      }
    }
    return new ArrayList<>(deleteRules);
  }

  private void ruleToDelete(Map.Entry<String, JsonNode> entry, Set<RuleTypeName> deleteRules) {
    RuleType ruleType;

    try {
      ruleType = RuleType.valueOf(entry.getKey());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Bad rule name '%s'", entry.getKey()));
    }

    JsonNode rules = entry.getValue().get("Rules");
    if (rules == null || !rules.isArray()) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Rules '%s' must be an array", rules));
    }

    for (JsonNode rule : rules) {
      if (!rule.isObject()) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Rule '%s' must be an object", rule));
      }
      JsonNode ruleName = rule.get("Rule");
      if (ruleName == null) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Rule name '%s' must be not null ", rule));
      }
      deleteRules.add(new RuleTypeName(ruleType, ruleName.asText()));
    }
  }

  //    "update": [ { "AccessRule": { "Rules": [
  //      { "OldRule": "ACC-00001", "Rule": "ACC-00002"},
  //      { "OldRule": "ACC-00003", "DeleteStartDate": true } ] } },
  private Map<RuleType, UpdateRules> getUpdateRules(List<JsonNode> rules) {
    Map<RuleType, UpdateRules> updateRules = new EnumMap<>(RuleType.class);
    if (rules != null) {
      for (JsonNode rule : rules) {
        if (!rule.isObject()) {
          throw new BadRequestException(
              CREATION_FAILED, String.format("Delete rule '%s' have to be an object", rule));
        }
        rule.fields().forEachRemaining(e -> ruleToUpdate(e, updateRules));
      }
    }
    return updateRules;
  }

  private void ruleToUpdate(
      Map.Entry<String, JsonNode> entry, Map<RuleType, UpdateRules> updateRules) {
    RuleType ruleType;

    try {
      ruleType = RuleType.valueOf(entry.getKey());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Bad rule name '%s'", entry.getKey()));
    }

    try {
      UpdateRules updateRuleAction = JsonService.to(entry.getValue(), UpdateRules.class);
      var value = updateRules.put(ruleType, updateRuleAction);
      if (value != null) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Rule '%s' cannot be defined twice", ruleType));
      }
    } catch (JsonProcessingException e) {
      throw new BadRequestException(CREATION_FAILED, e.getMessage());
    }
  }

  // "add": [ {
  //  "AccessRule": {
  //    "PreventInheritance": false,
  //    "Rules": [{ "Rule": "ACC-00003", "StartDate": "2018-11-14" } ] } } ]
  protected List<AbstractRules> getAddRules(List<JsonNode> rules) {
    Map<RuleType, AbstractRules> addRules = new EnumMap<>(RuleType.class);
    if (rules != null) {
      for (JsonNode rule : rules) {
        if (!rule.isObject()) {
          throw new BadRequestException(
              CREATION_FAILED, String.format("Add rule '%s' have to be an object", rule));
        }
        rule.fields().forEachRemaining(e -> ruleToAdd(e, addRules));
      }
    }
    return new ArrayList<>(addRules.values());
  }

  private void ruleToAdd(Map.Entry<String, JsonNode> entry, Map<RuleType, AbstractRules> addRules) {
    RuleType ruleType;

    try {
      ruleType = RuleType.valueOf(entry.getKey());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(
          CREATION_FAILED, String.format("Bad rule name '%s'", entry.getKey()));
    }

    try {
      AbstractRules arules =
          switch (ruleType) {
            case AppraisalRule -> JsonService.to(entry.getValue(), AppraisalRules.class);
            case AccessRule -> JsonService.to(entry.getValue(), AccessRules.class);
            case StorageRule -> JsonService.to(entry.getValue(), StorageRules.class);
            case DisseminationRule -> JsonService.to(entry.getValue(), DisseminationRules.class);
            case ClassificationRule -> JsonService.to(entry.getValue(), ClassificationRules.class);
            case ReuseRule -> JsonService.to(entry.getValue(), ReuseRules.class);
            case HoldRule -> JsonService.to(entry.getValue(), HoldRules.class);
          };
      var value = addRules.put(ruleType, arules);
      if (value != null) {
        throw new BadRequestException(
            CREATION_FAILED, String.format("Rule '%s' cannot be defined twice", ruleType));
      }
    } catch (JsonProcessingException e) {
      throw new BadRequestException(CREATION_FAILED, e.getMessage());
    }
  }
}
