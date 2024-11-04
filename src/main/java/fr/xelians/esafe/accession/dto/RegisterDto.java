/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.accession.domain.model.ValueDetail;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterDto(
    @JsonProperty("#tenant") Long tenant,
    @JsonProperty("#version") Long version,
    @JsonProperty("OriginatingAgency") String originatingAgency,
    @JsonProperty("CreationDate") LocalDateTime creationDate,
    @JsonProperty("TotalObjectGroups") ValueDetail totalObjectGroups,
    @JsonProperty("TotalUnits") ValueDetail totalUnits,
    @JsonProperty("TotalObjects") ValueDetail totalObjects,
    @JsonProperty("ObjectSize") ValueDetail objectSize) {}