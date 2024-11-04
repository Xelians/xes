/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationType;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LogbookOperationDto(
    @JsonProperty("#id") String id,
    @JsonProperty("#tenant") Long tenant,
    @JsonProperty("Type") OperationType type,
    @JsonProperty("UserIdentifier") String userIdentifier,
    @JsonProperty("ApplicationId") String applicationId,
    @JsonProperty("Created") LocalDateTime created,
    @JsonProperty("Modified") LocalDateTime modified,
    @JsonProperty("Outcome") String outcome,
    @JsonProperty("Message") String message) {}
