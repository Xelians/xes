/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.*;
import java.util.*;
import lombok.*;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.util.CollectionUtils;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DescriptiveMetadataDto {

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String id;

  @JsonProperty("Title")
  private String title;

  @JsonProperty("DescriptionLevel")
  private String descriptionLevel;

  @JsonProperty("Description")
  private String description;

  @JsonProperty("OriginatingAgencyArchiveUnitIdentifier")
  private List<String> originatingAgencyArchiveUnitIdentifier = new ArrayList<>();

  @JsonProperty("Status")
  private String status;

  @JsonProperty("TransactedDate")
  private String transactedDate;

  @JsonProperty("#nbunits")
  private Integer nbunits;

  @JsonProperty("#tenant")
  private Integer tenant;

  @JsonProperty("#object")
  private String unitObject;

  @JsonProperty("#unitups")
  private List<String> unitups = new ArrayList<>();

  @JsonProperty("#min")
  private Integer min;

  @JsonProperty("#max")
  private Integer max;

  @JsonProperty("#allunitups")
  private List<String> allunitups = new ArrayList<>();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private String unitType;

  @JsonProperty("#operations")
  private List<String> operations = new ArrayList<>();

  @JsonProperty("#opi")
  private String opi;

  @JsonProperty("#originating_agency")
  private String originating_agency;

  @JsonProperty("#originating_agencies")
  private List<String> originatingAgencies = new ArrayList<>();

  @JsonProperty("Version")
  private String version;

  @JsonProperty("#management")
  private ManagementDto management;

  @JsonProperty("InheritedRules")
  private InheritedRulesDto inheritedRules;

  @JsonProperty("#computedInheritedRules")
  private ComputedInheritedRulesDto computedInheritedRules;

  @JsonProperty("#validComputedInheritedRules")
  private boolean validComputedInheritedRules;

  @JsonProperty("DocumentType")
  private String documentType;

  @JsonProperty("StartDate")
  private String startDate;

  @JsonProperty("EndDate")
  private String endDate;

  @JsonProperty("ReceivedDate")
  private String receivedDate;

  @JsonProperty("CreatedDate")
  private String createdDate;

  @JsonProperty("AcquiredDate")
  private String acquiredDate;

  @JsonProperty("SentDate")
  private String sentDate;

  @JsonProperty("RegisteredDate")
  private String registeredDate;

  @JsonProperty("Xtag")
  private List<XtagDto> xtag = new ArrayList<>();

  @JsonProperty("Vtag")
  private List<VtagDto> vtag = new ArrayList<>();

  @JsonProperty("#storage")
  private StorageDto storage;

  @JsonProperty("#nbobjects")
  private Integer nbobjects;

  @JsonProperty("FileInfo")
  private FileInfoDto fileInfo;

  @JsonProperty("#qualifiers")
  private List<QualifiersDto> qualifiers = new ArrayList<>();

  @JsonProperty("SubmissionAgency")
  private IdentifierDto submissionAgency;

  @JsonProperty("OriginatingSystemId")
  private List<String> originatingSystemId = new ArrayList<>();

  @JsonProperty("PhysicalAgency")
  private List<String> physicalAgency = new ArrayList<>();

  @JsonProperty("PhysicalStatus")
  private List<String> physicalStatus = new ArrayList<>();

  @JsonProperty("PhysicalReference")
  private List<String> physicalReference = new ArrayList<>();

  @JsonProperty("PhysicalBarcode")
  private List<String> physicalBarcode = new ArrayList<>();

  @JsonProperty("PhysicalType")
  private List<String> physicalType = new ArrayList<>();

  @JsonProperty("PhysicalIdentifier")
  private List<String> physicalIdentifier = new ArrayList<>();

  @JsonProperty("Keyword")
  private List<KeywordDto> keyword = new ArrayList<>();

  @JsonProperty("FilePlanPosition")
  private List<String> filePlanPosition = new ArrayList<>();

  @JsonProperty("SystemId")
  private List<String> systemId = new ArrayList<>();

  @JsonProperty("ArchivalAgencyArchiveUnitIdentifier")
  private List<String> archivalAgencyArchiveUnitIdentifier = new ArrayList<>();

  @JsonProperty("TransferringAgencyArchiveUnitIdentifier")
  private List<String> transferringAgencyArchiveUnitIdentifier = new ArrayList<>();

  @JsonProperty("CustodialHistory")
  private CustodialHistoryModelDto custodialHistory;

  @JsonProperty("Type")
  private String type;

  @JsonProperty("Language")
  private List<String> language = new ArrayList<>();

  @JsonProperty("DescriptionLanguage")
  private String descriptionLanguage;

  @JsonProperty("Tag")
  private List<String> tag = new ArrayList<>();

  @JsonProperty("Coverage")
  private String coverage;

  @JsonProperty("OriginatingAgency")
  private IdentifierDto originatingAgency;

  @JsonProperty("AuthorizedAgent")
  private List<AgentTypeDto> authorizedAgent = new ArrayList<>();

  @JsonProperty("Writer")
  private List<AgentTypeDto> writer = new ArrayList<>();

  @JsonProperty("Addressee")
  private List<AgentTypeDto> addressee = new ArrayList<>();

  @JsonProperty("Recipient")
  private List<AgentTypeDto> recipient = new ArrayList<>();

  @JsonProperty("Transmitter")
  private List<AgentTypeDto> transmitter = new ArrayList<>();

  @JsonProperty("Sender")
  private List<AgentTypeDto> sender = new ArrayList<>();

  @JsonProperty("Source")
  private String source;

  @JsonProperty("parentId")
  private String parentId;

  @JsonIgnore private Map<String, Object> anyProperties = new HashMap<>();

  @JsonProperty("#id")
  public String getId() {
    return id;
  }

  @JsonProperty("#id")
  public void setId(final String id) {
    this.id = id;
  }

  @Deprecated
  @JsonProperty("id")
  private void setIdV2(final String id) {
    if (this.id == null) {
      setId(id);
    }
  }

  public List<String> getUnitups() {
    return unitups;
  }

  public void setUnitups(final List<String> unitups) {
    this.unitups = unitups;
  }

  public List<String> getAllunitups() {
    return allunitups;
  }

  public void setAllunitups(final List<String> allunitups) {
    this.allunitups = allunitups;
  }

  @JsonProperty("#unitType")
  public String getUnitType() {
    return unitType;
  }

  @JsonProperty("#unitType")
  public void setUnitType(final String unitType) {
    this.unitType = unitType;
  }

  @Deprecated
  @JsonProperty("unitType")
  private void setUnitTypeV2(final String unitType) {
    if (this.unitType == null) {
      setUnitType(unitType);
    }
  }

  public Optional<AgentTypeDto> retrieveFirstTransmitter() {
    return Optional.ofNullable(transmitter).orElse(Collections.emptyList()).stream().findFirst();
  }

  public Optional<String> retrieveFirstPhysicalAgency() {
    return findFirstElement(physicalAgency);
  }

  public Optional<String> retrieveFirstPhysicalBarcode() {
    return findFirstElement(physicalBarcode);
  }

  public Optional<String> retrieveFirstPhysicalStatus() {
    return findFirstElement(physicalStatus);
  }

  public Optional<String> retrieveFirstPhysicalType() {
    return findFirstElement(physicalType);
  }

  public Optional<String> retrieveFirstPhysicalIdentifier() {
    return findFirstElement(physicalIdentifier);
  }

  public Optional<String> retrieveFirstOriginatingAgencyArchiveUnitIdentifier() {
    return findFirstElement(originatingAgencyArchiveUnitIdentifier);
  }

  private Optional<String> findFirstElement(final List<String> elements) {
    return Optional.ofNullable(elements).orElse(Collections.emptyList()).stream().findFirst();
  }

  @JsonAnyGetter
  public Map<String, Object> getAnyProperties() {
    return anyProperties;
  }

  @JsonIgnore
  private void setAnyProperties(final Map<String, Object> anyProperties) {
    this.anyProperties = anyProperties;
  }

  @JsonAnySetter
  public void addToAnyProperties(final String key, final Object value) {
    // Technical fields are excluded
    if (key != null && key.startsWith("#")) {
      return;
    }
    anyProperties.put(key, value);
  }

  @JsonIgnore
  public boolean isPhysicalStatus(final String physicalStatus) {
    return StringUtils.equals(physicalStatus, CollectionUtils.firstElement(this.physicalStatus));
  }

  @JsonIgnore
  public boolean isHybridStatus() {

    final boolean hybridQualifier =
        Objects.nonNull(qualifiers)
            && qualifiers.stream()
                .anyMatch((qualifierDto -> qualifierDto.getQualifier().equals("BinaryMaster")));

    boolean hybridDescriptionLevel =
        Objects.nonNull(descriptionLevel) && descriptionLevel.equals("Item");
    return !CollectionUtils.isEmpty(physicalType)
        && Objects.nonNull(unitObject)
        && hybridQualifier
        && hybridDescriptionLevel;
  }
}
