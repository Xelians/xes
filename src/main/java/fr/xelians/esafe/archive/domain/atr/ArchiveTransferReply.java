/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.atr;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.unit.Message;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArchiveTransferReply extends Message {

  @NotNull
  @JsonProperty("Type")
  protected ReportType type;

  @NotNull
  @JsonProperty("Tenant")
  protected Long tenant;

  @NotNull
  @JsonProperty("OperationId")
  protected Long operationId;

  @JsonProperty("ArchivalAgreement")
  protected String archivalAgreement;

  @JsonProperty("ArchivalAgencyIdentifier")
  protected String archivalAgencyIdentifier;

  @JsonProperty("TransferringAgencyIdentifier")
  protected String transferringAgencyIdentifier;

  @JsonProperty("ArchivalProfile")
  protected String archivalProfile;

  @JsonProperty("AcquisitionInformation")
  protected String acquisitionInformation;

  @JsonProperty("ServiceLevel")
  protected String serviceLevel;

  @JsonProperty("LegalStatus")
  protected String legalStatus;

  @JsonProperty("GrantDate")
  protected LocalDateTime grantDate = LocalDateTime.now();

  @JsonProperty("DataObjectGroups")
  protected List<DataObjectGroupReply> dataObjectGroupReplys;

  @JsonProperty("ArchiveUnits")
  protected List<ArchiveUnitReply> archiveUnitReplys;

  @NotNull
  @JsonProperty("ReplyCode")
  protected String replyCode;

  @JsonProperty("MessageRequestIdentifier")
  protected String messageRequestIdentifier;

  @NotNull
  @JsonProperty("NumOfUnits")
  protected Integer numOfUnits;

  @NotNull
  @JsonProperty("NumOfObjectGroups")
  protected Integer numOfObjectGroups;

  @NotNull
  @JsonProperty("NumOfPhysicalObjects")
  protected Integer numOfPhysicalObjects;

  @NotNull
  @JsonProperty("NumOfBinaryObjects")
  protected Integer numOfBinaryObjects;

  @NotNull
  @JsonProperty("SizeOfBinaryObjects")
  protected Long sizeOfBinaryObjects;

  @JsonIgnore
  public Map<String, ArchiveUnitReply> getArchiveUnitReplyMap() {
    return archiveUnitReplys.stream()
        .collect(toMap(ArchiveUnitReply::getXmlId, Function.identity()));
  }

  @JsonIgnore
  public Map<String, PhysicalDataObjectReply> getPhysicalDataObjectMap() {
    return dataObjectGroupReplys.stream()
        .flatMap(d -> d.getPhysicalDataObjects().stream())
        .collect(toMap(PhysicalDataObjectReply::getXmlId, Function.identity()));
  }

  @JsonIgnore
  public Map<String, BinaryDataObjectReply> getBinaryDataObjectReplyMap() {
    return dataObjectGroupReplys.stream()
        .flatMap(d -> d.getBinaryDataObjects().stream())
        .collect(toMap(BinaryDataObjectReply::getXmlId, Function.identity()));
  }
}
