package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.accession.domain.model.ValueDetail;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterSummaryDto(
    @JsonProperty("#tenant") Long tenant,
    @JsonProperty("#version") Long version,
    @JsonProperty("OriginatingAgency") String originatingAgency,
    @JsonProperty("CreationDate") LocalDateTime creationDate,
    @JsonProperty("TotalObjectGroups") ValueDetail totalObjectsGroups,
    @JsonProperty("TotalUnits") ValueDetail totalUnits,
    @JsonProperty("TotalObjects") ValueDetail totalObjects,
    @JsonProperty("ObjectSize") ValueDetail objectSize) {}
