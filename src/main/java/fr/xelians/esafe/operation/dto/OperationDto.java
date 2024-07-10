/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record OperationDto(
    @JsonProperty("Id") @NotNull String id,
    @JsonProperty("Tenant") @NotNull Long tenant,
    @JsonProperty("Type") @NotNull OperationType type,
    @JsonProperty("Status") @NotNull OperationStatus status,
    @JsonProperty("Message") String message,
    @JsonProperty("Events") String events,
    @JsonProperty("UserIdentifier") @NotNull String userIdentifier,
    @JsonProperty("ApplicationId") String applicationId,
    @JsonProperty("Created") @NotNull LocalDateTime created,
    @JsonProperty("Modified") @NotNull LocalDateTime modified) {}
