package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValueDetailDto(
    @JsonProperty("ingested") Long ingested,
    @JsonProperty("deleted") Long deleted,
    @JsonProperty("remained") Long remained) {}
