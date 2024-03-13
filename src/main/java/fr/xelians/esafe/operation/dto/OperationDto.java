/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record OperationDto(
    @JsonProperty("Id") @NotNull Long id,
    @JsonProperty("Tenant") @NotNull Long tenant,
    @JsonProperty("Type") @NotNull OperationType type,
    @JsonProperty("Status") @NotNull OperationStatus status,
    @JsonProperty("Message") String message,
    @JsonProperty("Events") String events,
    @JsonProperty("UserIdentifier") @NotNull String userIdentifier,
    @JsonProperty("ApplicationId") String applicationId,
    @JsonProperty("Created") @NotNull LocalDateTime created,
    @JsonProperty("Modified") @NotNull LocalDateTime modified) {}
