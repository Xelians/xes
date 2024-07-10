/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.*;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.referential.domain.RuleType;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ComputedInheritedRules {

  @JsonProperty("AccessRule")
  protected AccessComputedRules accessComputedRules;

  @JsonProperty("AppraisalRule")
  protected AppraisalComputedRules appraisalComputedRules;

  @JsonProperty("DisseminationRule")
  protected DisseminationComputedRules disseminationComputedRules;

  @JsonProperty("ReuseRule")
  protected ReuseComputedRules reuseComputedRules;

  @JsonProperty("ClassificationRule")
  protected ClassificationComputedRules classificationComputedRules;

  @JsonProperty("StorageRule")
  protected StorageComputedRules storageComputedRules;

  @JsonProperty("HoldRule")
  protected HoldComputedRules holdComputedRules;

  @JsonIgnore
  public void setAppraisalRules(AppraisalRules arules) {
    appraisalComputedRules = new AppraisalComputedRules();
    appraisalComputedRules.setFinalAction(arules.getFinalAction());
    appraisalComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      appraisalComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
  }

  @JsonIgnore
  public void setAccessRules(AccessRules arules) {
    accessComputedRules = new AccessComputedRules();
    accessComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      accessComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
  }

  @JsonIgnore
  public void setClassificationRules(ClassificationRules arules) {
    classificationComputedRules = new ClassificationComputedRules();
    classificationComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      classificationComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
    if (arules.getClassificationAudience() != null) {
      classificationComputedRules.setClassificationAudiences(
          List.of(arules.getClassificationAudience()));
    }
    if (arules.getClassificationLevel() != null) {
      classificationComputedRules.setClassificationLevels(List.of(arules.getClassificationLevel()));
    }
    if (arules.getClassificationOwner() != null) {
      classificationComputedRules.setClassificationOwners(List.of(arules.getClassificationOwner()));
    }
    if (arules.getClassificationReassessingDate() != null) {
      classificationComputedRules.setClassificationReassessingDates(
          List.of(arules.getClassificationReassessingDate()));
    }
    if (arules.getNeedReassessingAuthorization() != null) {
      classificationComputedRules.setNeedReassessingAuthorizations(
          List.of(arules.getNeedReassessingAuthorization()));
    }
  }

  @JsonIgnore
  public void setDisseminationRules(DisseminationRules arules) {
    disseminationComputedRules = new DisseminationComputedRules();
    disseminationComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      disseminationComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
  }

  @JsonIgnore
  public void setReuseRules(ReuseRules arules) {
    reuseComputedRules = new ReuseComputedRules();
    reuseComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      reuseComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
  }

  @JsonIgnore
  public void setStorageRules(StorageRules arules) {
    storageComputedRules = new StorageComputedRules();
    storageComputedRules.setFinalAction(arules.getFinalAction());
    storageComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      storageComputedRules.rules.add(
          new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
  }

  @JsonIgnore
  public void setHoldRules(HoldRules arules) {
    holdComputedRules = new HoldComputedRules();
    holdComputedRules.setMaxEndDate(arules.getEndDate());
    for (Rule rule : arules.getRules()) {
      holdComputedRules.rules.add(new ComputedInheritedRule(rule.getRuleName(), rule.getEndDate()));
    }
    holdComputedRules.setPreventRearrangement(arules.getPreventRearrangement());
  }

  @JsonIgnore
  public void setRules(AbstractComputedRules abstractRules) {
    switch (abstractRules) {
      case AppraisalComputedRules rule -> this.appraisalComputedRules = rule;
      case AccessComputedRules rule -> this.accessComputedRules = rule;
      case DisseminationComputedRules rule -> this.disseminationComputedRules = rule;
      case ReuseComputedRules rule -> this.reuseComputedRules = rule;
      case ClassificationComputedRules rule -> this.classificationComputedRules = rule;
      case StorageComputedRules rule -> this.storageComputedRules = rule;
      case HoldComputedRules rule -> this.holdComputedRules = rule;
      default -> throw new InternalException(
          String.format("Bad abstractRule: %s", abstractRules.getClass().getSimpleName()));
    }
  }

  @JsonIgnore
  public AbstractComputedRules getRules(RuleType ruleType) {
    return switch (ruleType) {
      case ReuseRule -> reuseComputedRules;
      case AccessRule -> accessComputedRules;
      case AppraisalRule -> appraisalComputedRules;
      case ClassificationRule -> classificationComputedRules;
      case HoldRule -> holdComputedRules;
      case StorageRule -> storageComputedRules;
      case DisseminationRule -> disseminationComputedRules;
    };
  }
}
