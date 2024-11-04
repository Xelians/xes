/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.dto;

import static fr.xelians.esafe.common.constant.DefaultValue.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.common.constraint.NoHtml;
import fr.xelians.esafe.common.constraint.RegularChar;
import fr.xelians.esafe.referential.domain.CheckParentLinkStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngestContractDto extends AbstractReferentialDto {

  @Min(value = 0)
  @JsonProperty("LinkParentId")
  private Long linkParentId;

  @Size(max = 1000)
  @JsonProperty("CheckParentId")
  private HashSet<Long> checkParentIds = new HashSet<>();

  @JsonProperty("CheckParentLink")
  private CheckParentLinkStatus checkParentLink = CHECK_PARENT_LINK_STATUS;

  @Size(max = 1000)
  @JsonProperty("ArchiveProfiles")
  private HashSet<String> archiveProfiles = new HashSet<>();

  @JsonProperty("MasterMandatory")
  private Boolean masterMandatory = MASTER_MANDATORY;

  @JsonProperty("EveryDataObjectVersion")
  private Boolean everyDataObjectVersion = EVERY_DATA_OBJECT_VERSION;

  @Size(max = 1000)
  @JsonProperty("DataObjectVersion")
  private HashSet<String> dataObjectVersion = new HashSet<>();

  @JsonProperty("FormatUnidentifiedAuthorized")
  private Boolean formatUnidentifiedAuthorized = FORMAT_UNIDENTIFIED_AUTHORIZED;

  @JsonProperty("EveryFormatType")
  private Boolean everyFormatType = EVERY_FORMAT_TYPE;

  @Size(max = 1000)
  @JsonProperty("FormatType")
  private HashSet<String> formatType = new HashSet<>();

  @JsonProperty("ComputeInheritedRulesAtIngest")
  private Boolean computeInheritedRulesAtIngest = COMPUTE_INHERITED_RULES_AT_INGEST;

  @NoHtml
  @RegularChar
  @JsonProperty("ManagementContractId")
  private String managementContractId;

  @JsonProperty("StoreManifest")
  private Boolean storeManifest = DefaultValue.STORE_MANIFEST;
}
