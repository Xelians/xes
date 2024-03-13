/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.elimination;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.constraint.JsonSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EliminationQuery(
    @JsonProperty("$roots") @Size(max = 1024) List<Long> roots,
    @JsonProperty("$type") @Size(max = 1024) String type,
    @NotNull @JsonProperty("$query") @JsonSize JsonNode queryNode,
    @JsonProperty("$threshold") Long threshold,
    @JsonProperty("$filter") @JsonSize JsonNode filterNode) {}
