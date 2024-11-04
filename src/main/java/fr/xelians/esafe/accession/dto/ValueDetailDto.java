/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * @author Emmanuel Deviller
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValueDetailDto(
    @JsonProperty("ingested") Long ingested,
    @JsonProperty("deleted") Long deleted,
    @JsonProperty("remained") Long remained) {}
