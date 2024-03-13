/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import jakarta.validation.constraints.NotNull;

public record UpdateRuleQuery(
    @NotNull @JsonProperty("dslRequest") SearchQuery searchQuery,
    @NotNull @JsonProperty("ruleActions") RuleActions ruleActions) {}
