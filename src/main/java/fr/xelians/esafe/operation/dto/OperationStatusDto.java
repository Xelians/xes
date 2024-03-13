/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationStatus;

public record OperationStatusDto(
    @JsonProperty("Id") Long id,
    @JsonProperty("Status") OperationStatus status,
    @JsonProperty("Message") String message) {}
