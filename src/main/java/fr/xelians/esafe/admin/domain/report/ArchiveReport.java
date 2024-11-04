/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NonNull;

/*
 * @author Emmanuel Deviller
 */
public record ArchiveReport(
    @NonNull @JsonProperty("Type") ReportType type,
    @NonNull @JsonProperty("Date") LocalDateTime date,
    @NonNull @JsonProperty("Tenant") Long tenant,
    @NonNull @JsonProperty("OperationId") Long operationId,
    @NonNull @JsonProperty("GrantDate") LocalDateTime grantDate,
    @NonNull @JsonProperty("Status") ReportStatus status,
    @NonNull @JsonProperty("ArchiveUnits") List<ArchiveUnit> archiveUnits,
    @NonNull @JsonProperty("NumOfUnits") Integer numOfUnits,
    @NonNull @JsonProperty("NumOfObjectGroups") Integer numOfObjectGroups,
    @NonNull @JsonProperty("NumOfPhysicalObjects") Integer numOfPhysicalObjects,
    @NonNull @JsonProperty("NumOfBinaryObjects") Integer numOfBinaryObjects,
    @NonNull @JsonProperty("SizeOfBinaryObjects") Long sizeOfBinaryObjects) {

  public record ArchiveUnit(
      @NonNull @JsonProperty("SystemId") String systemId,
      @NonNull @JsonProperty("OperationId") Long operationId,
      @NonNull @JsonProperty("ArchivalAgencyIdentifier") String archivalAgencyIdentifier,
      @NonNull @JsonProperty("ArchivalAgencyIdentifiers") List<String> archivalAgencyIdentifiers,
      @NonNull @JsonProperty("CreationDate") LocalDateTime creationDate,
      @JsonProperty("BinaryDataObjects") List<BinaryDataObject> binaryDataObjects,
      @JsonProperty("PhysicalDataObjects") List<PhysicalDataObject> physicalDataObjects) {

    @JsonIgnore
    public long getTotalObjects() {
      return (long) binaryDataObjects().size() + (long) physicalDataObjects().size();
    }

    @JsonIgnore
    public long getSizeOfBinaryObjects() {
      return binaryDataObjects().stream().mapToLong(BinaryDataObject::size).sum();
    }
  }

  public record BinaryDataObject(
      @NonNull @JsonProperty("DataObjectSystemId") String dataObjectSystemId,
      @NonNull @JsonProperty("DataObjectVersion") String dataObjectVersion,
      @JsonProperty("Size") long size,
      @NonNull @JsonProperty("MessageDigest") String messageDigest,
      @NonNull @JsonProperty("DigestAlgorithm") String digestAlgorithm) {}

  public record PhysicalDataObject(
      @NonNull @JsonProperty("DataObjectSystemId") String dataObjectSystemId,
      @NonNull @JsonProperty("DataObjectVersion") String dataObjectVersion) {}
}
