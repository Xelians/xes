/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValueEventDto(
    @JsonProperty("Opc") String operationId,
    @JsonProperty("OpType") String operationType,
    @JsonProperty("Gots") Long objectsGroups,
    @JsonProperty("Units") Long totalUnits,
    @JsonProperty("Objects") Long totalObjects,
    @JsonProperty("ObjSize") Long objectSize,
    @JsonProperty("CreationDate") LocalDateTime creationDate) {}
