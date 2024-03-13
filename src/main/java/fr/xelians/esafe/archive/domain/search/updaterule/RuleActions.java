/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RuleActions(
    @JsonProperty("add") @Size(max = 64) List<JsonNode> add,
    @JsonProperty("update") @Size(max = 64) List<JsonNode> update,
    @JsonProperty("delete") @Size(max = 64) List<JsonNode> delete) {}
