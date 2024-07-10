/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

public record EliminationReportDto(
    @NonNull @JsonProperty("Type") ReportType type,
    @NonNull @JsonProperty("Date") LocalDateTime date,
    @NonNull @JsonProperty("Tenant") Long tenant,
    @NonNull @JsonProperty("OperationId") Long operationId,
    @NonNull @JsonProperty("GrantDate") LocalDateTime grantDate,
    @NonNull @JsonProperty("Status") ReportStatus status,
    @NonNull @JsonProperty("ArchiveUnits") List<ArchiveUnitDto> archiveUnits,
    @NonNull @JsonProperty("NumOfUnits") Integer numOfUnits,
    @NonNull @JsonProperty("NumOfObjectGroups") Integer numOfObjectGroups,
    @NonNull @JsonProperty("NumOfPhysicalObjects") Integer numOfPhysicalObjects,
    @NonNull @JsonProperty("NumOfBinaryObjects") Integer numOfBinaryObjects,
    @NonNull @JsonProperty("SizeOfBinaryObjects") Long sizeOfBinaryObjects) {

  public record ArchiveUnitDto(
      @NonNull @JsonProperty("SystemId") String systemId,
      @NonNull @JsonProperty("OperationId") Long operationId,
      @NonNull @JsonProperty("ArchivalAgencyIdentifier") String archivalAgencyIdentifier,
      @NonNull @JsonProperty("CreationDate") LocalDateTime creationDate,
      @NonNull @JsonProperty("BinaryDataObjects") List<BinaryDataObjectDto> binaryDataObjects,
      @NonNull @JsonProperty("PhysicalDataObjects")
          List<PhysicalDataObjectDto> physicalDataObjects) {

    public long getTotalObjects() {
      return (long) binaryDataObjects().size() + (long) physicalDataObjects().size();
    }

    public long getSizeOfBinaryObjects() {
      return binaryDataObjects().stream().mapToLong(BinaryDataObjectDto::size).sum();
    }
  }

  public record BinaryDataObjectDto(
      @NonNull @JsonProperty("DataObjectSystemId") String dataObjectSystemId,
      @NonNull @JsonProperty("DataObjectVersion") String dataObjectVersion,
      @JsonProperty("Size") long size,
      @NonNull @JsonProperty("MessageDigest") String messageDigest,
      @NonNull @JsonProperty("DigestAlgorithm") String digestAlgorithm) {}

  public record PhysicalDataObjectDto(
      @NonNull @JsonProperty("DataObjectSystemId") String dataObjectSystemId,
      @NonNull @JsonProperty("DataObjectVersion") String dataObjectVersion) {}
}
