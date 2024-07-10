/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.inherited.InheritedHoldRule;
import fr.xelians.esafe.archive.domain.unit.rules.inherited.InheritedRule;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HoldRule extends Rule {

  @JsonProperty("HoldEndDate")
  protected LocalDate holdEndDate;

  @JsonProperty("HoldOwner")
  protected String holdOwner;

  @JsonProperty("HoldReason")
  protected String holdReason;

  @JsonProperty("HoldReassessingDate")
  protected LocalDate holdReassessingDate;

  @JsonInclude
  @JsonProperty("PreventRearrangement")
  protected Boolean preventRearrangement = Boolean.FALSE;

  public HoldRule(@JsonProperty("Rule") String ruleName) {
    super(ruleName);
  }

  @JsonCreator
  public HoldRule(
      @JsonProperty("Rule") String name,
      @JsonProperty("StartDate") LocalDate startDate,
      @JsonProperty("HoldEndDate") LocalDate holdEndDate,
      @JsonProperty("HoldOwner") String holdOwner,
      @JsonProperty("HoldReason") String holdReason,
      @JsonProperty("HoldReassessingDate") LocalDate holdReassessingDate,
      @JsonProperty("PreventRearrangement") Boolean preventRearrangement) {

    super(name, startDate);
    this.holdOwner = holdOwner;
    this.holdReason = holdReason;
    this.holdEndDate = holdEndDate;
    this.holdReassessingDate = holdReassessingDate;
    this.preventRearrangement = preventRearrangement;
  }

  @Override
  @JsonIgnore
  public InheritedRule createInheritedRule() {
    InheritedHoldRule inheritedRule = new InheritedHoldRule();
    inheritedRule.setRule(ruleName);
    inheritedRule.setStartDate(startDate);
    inheritedRule.setEndDate(endDate);
    inheritedRule.setHoldEndDate(holdEndDate);
    inheritedRule.setHoldOwner(holdOwner);
    inheritedRule.setHoldReason(holdReason);
    inheritedRule.setHoldReassessingDate(holdReassessingDate);
    inheritedRule.setPreventRearrangement(preventRearrangement);
    return inheritedRule;
  }
}
