/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationType;
import java.time.LocalDateTime;

public record LifeCycle(
    @JsonProperty("_av") int autoVersion,
    @JsonProperty("_opi") long operationId,
    @JsonProperty("_opType") OperationType operationType,
    @JsonProperty("_opDate") LocalDateTime operationDate,
    @JsonProperty("_patch") String patch) {}
