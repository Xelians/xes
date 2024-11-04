/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ManagementMetadata {

  @JsonProperty("ArchivalProfile")
  protected String archivalProfile;

  @JsonProperty("ServiceLevel")
  protected String serviceLevel;

  @JsonProperty("AcquisitionInformation")
  protected String acquisitionInformation;

  @JsonProperty("LegalStatus")
  protected String legalStatus;

  @JsonProperty("OriginatingAgencyIdentifier")
  protected String originatingAgencyIdentifier;

  @JsonProperty("SubmissionAgencyIdentifier")
  protected String submissionAgencyIdentifier;
}
