/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UnitRuleDto {

  /** Unit id */
  @JsonProperty("UnitId")
  private String unitId;

  /** Originating Agency Name */
  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  /** Rule id */
  @JsonProperty("Rule")
  private String rule;

  /** Start date */
  @JsonProperty("StartDate")
  private String startDate;

  /** End date */
  @JsonProperty("EndDate")
  private String endDate;

  /** Paths */
  @JsonProperty("Paths")
  private List<ArrayList<String>> paths = new ArrayList<>();

  /** Delete Start Date */
  @JsonProperty("DeleteStartDate")
  private Boolean deleteStartDate;

  /** Hold End Date */
  @JsonProperty("HoldEndDate")
  private String holdEndDate;

  /** Delete Hold EndDate */
  @JsonProperty("DeleteHoldEndDate")
  private Boolean deleteHoldEndDate;

  /** Hold Owner */
  @JsonProperty("HoldOwner")
  private String holdOwner;

  /** Delete Hold Owner */
  @JsonProperty("DeleteHoldOwner")
  private Boolean deleteHoldOwner;

  /** Hold Reason */
  @JsonProperty("HoldReason")
  private String holdReason;

  /** Delete Hold Reason */
  @JsonProperty("DeleteHoldReason")
  private Boolean deleteHoldReason;

  /** Hold Reassessing Date */
  @JsonProperty("HoldReassessingDate")
  private String holdReassessingDate;

  /** Delete Hold Reassessing Date */
  @JsonProperty("DeleteHoldReassessingDate")
  private Boolean deleteHoldReassessingDate;

  /** Prevent Rearrangement */
  @JsonProperty("PreventRearrangement")
  private Boolean preventRearrangement;

  /** Delete Prevent Rearrangement */
  @JsonProperty("DeletePreventRearrangement")
  private Boolean deletePreventRearrangement;
}
