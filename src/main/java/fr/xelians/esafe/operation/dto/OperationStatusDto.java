/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationStatus;

/*
 * @author Emmanuel Deviller
 */
public record OperationStatusDto(
    @JsonProperty("Id") String id,
    @JsonProperty("Status") OperationStatus status,
    @JsonProperty("Message") String message) {}
