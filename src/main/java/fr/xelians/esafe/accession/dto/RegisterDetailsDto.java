package fr.xelians.esafe.accession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.accession.domain.model.RegisterStatus;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisterDetailsDto(
    @JsonProperty("#tenant") Long tenant,
    @JsonProperty("#version") Long version,
    @JsonProperty("OriginatingAgency") String originatingAgency,
    @JsonProperty("ArchivalProfile") String archivalProfile,
    @JsonProperty("SubmissionAgency") String submissionAgency,
    @JsonProperty("ArchivalAgreement") String archivalAgreement,
    @JsonProperty("AcquisitionInformation") String acquisitionInformation,
    @JsonProperty("LegalStatus") String legalStatus,
    @JsonProperty("EndDate") LocalDateTime endDate,
    @JsonProperty("StartDate") LocalDateTime startDate,
    @JsonProperty("LastUpdate") LocalDateTime lastUpdate,
    @JsonProperty("Status") RegisterStatus status,
    @JsonProperty("TotalObjectGroups") ValueDetailDto totalObjectsGroups,
    @JsonProperty("TotalUnits") ValueDetailDto totalUnits,
    @JsonProperty("TotalObjects") ValueDetailDto totalObjects,
    @JsonProperty("ObjectSize") ValueDetailDto objectSize,

    // Last operation ?
    @JsonProperty("Opc") String opc,

    // First operation ?
    @JsonProperty("Opi") String opi,

    // First operation type ?
    @JsonProperty("OpType") String operationType,

    // Operation ingest (origin of creation of the current detail
    @JsonProperty("Events") List<ValueEventDto> events,
    @JsonProperty("OperationIds") List<String> operationIds,
    @JsonProperty("obIdIn") String obIdIn,
    @JsonProperty("Comments") List<String> comments) {}
