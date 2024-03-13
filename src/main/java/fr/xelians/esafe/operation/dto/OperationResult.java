/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import java.util.List;

public record OperationResult<T>(
    @JsonProperty("httpCode") int httpCode,
    @JsonProperty("$hits") Hits hits,
    @JsonProperty("$results") List<T> results,
    @JsonProperty("$context") String context) {}
