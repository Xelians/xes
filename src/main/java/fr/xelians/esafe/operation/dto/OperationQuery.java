/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import fr.xelians.esafe.common.dto.LocalDateTimeDeserializer;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationType;
import java.time.LocalDateTime;
import java.util.Set;

/*
 * @author Emmanuel Deviller
 */
@JsonIgnoreProperties(value = {"workflows", "listSteps", "listProcessTypes"})
public record OperationQuery(
    @JsonProperty("id") String id,
    @JsonProperty("states") Set<OperationState> states,
    @JsonProperty("statuses") Set<String> statuses,
    @JsonProperty("types") Set<OperationType> types,
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("startDateMin")
        LocalDateTime startDateMin,
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @JsonProperty("startDateMax")
        LocalDateTime startDateMax) {}
