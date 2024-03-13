/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.xelians.esafe.common.dto.CustomLocalDateTimeDeserializer;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationType;
import java.time.LocalDateTime;
import java.util.Set;

@JsonIgnoreProperties(value = {"workflows", "listSteps", "listProcessTypes"})
public record OperationQuery(
    @JsonProperty("id") String id,
    @JsonProperty("states") Set<OperationState> states,
    @JsonProperty("statuses") Set<String> statuses,
    @JsonProperty("types") Set<OperationType> types,
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("startDateMin")
        LocalDateTime startDateMin,
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("startDateMax")
        LocalDateTime startDateMax) {}
