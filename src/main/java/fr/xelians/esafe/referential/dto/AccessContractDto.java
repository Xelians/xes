/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.referential.domain.Status;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AccessContractDto extends AbstractReferentialDto {

  @Size(max = 1000)
  @JsonProperty("RootUnits")
  private HashSet<Long> rootUnits = new HashSet<>();

  @Size(max = 1000)
  @JsonProperty("ExcludedRootUnits")
  private HashSet<Long> excludedRootUnits = new HashSet<>();

  @Size(max = 1000)
  @JsonProperty("DataObjectVersion")
  private HashSet<String> dataObjectVersion = new HashSet<>();

  @JsonProperty("EveryDataObjectVersion")
  private Boolean everyDataObjectVersion = DefaultValue.EVERY_DATA_OBJECT_VERSION;

  @Size(max = 1000)
  @JsonProperty("OriginatingAgencies")
  private HashSet<String> originatingAgencies = new HashSet<>();

  @JsonProperty("EveryOriginatingAgency")
  private Boolean everyOriginatingAgency = DefaultValue.EVERY_ORIGINATING_AGENCY;

  @JsonProperty("WritingPermission")
  private Boolean writingPermission = DefaultValue.WRITING_PERMISSION;

  @JsonProperty("WritingRestrictedDesc")
  private Boolean writingRestrictedDesc = DefaultValue.WRITING_RESTRICTED_DESC;

  @JsonProperty("AccessLog")
  private Status accessLog = DefaultValue.ACCESS_LOG;
}
