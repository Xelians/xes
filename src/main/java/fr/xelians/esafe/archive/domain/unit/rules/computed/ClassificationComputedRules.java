/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.referential.domain.RuleType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClassificationComputedRules extends AbstractComputedRules {

  @JsonProperty("ClassificationAudience")
  protected List<String> classificationAudiences = new ArrayList<>();

  @JsonProperty("ClassificationLevel")
  protected List<String> classificationLevels = new ArrayList<>();

  @JsonProperty("ClassificationOwner")
  protected List<String> classificationOwners = new ArrayList<>();

  @JsonProperty("ClassificationReassessingDate")
  protected List<LocalDate> classificationReassessingDates = new ArrayList<>();

  @JsonProperty("NeedReassessingAuthorization")
  protected List<Boolean> needReassessingAuthorizations = new ArrayList<>();

  public ClassificationComputedRules duplicate() {
    ClassificationComputedRules rules = new ClassificationComputedRules();
    rules.maxEndDate = this.maxEndDate;
    rules.inheritanceOrigin = this.inheritanceOrigin;
    rules.rules = new ArrayList<>(this.rules);
    rules.classificationAudiences = this.classificationAudiences;
    rules.classificationLevels = this.classificationLevels;
    rules.classificationOwners = this.classificationOwners;
    rules.classificationReassessingDates = this.classificationReassessingDates;
    rules.needReassessingAuthorizations = this.needReassessingAuthorizations;
    return rules;
  }

  @Override
  public RuleType getRuleType() {
    return RuleType.ClassificationRule;
  }
}
