/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
