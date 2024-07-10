/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InheritedRulesDto {

  @JsonProperty("AppraisalRule")
  private InheritedRuleCategoryDto appraisalRule;

  @JsonProperty("HoldRule")
  private InheritedRuleCategoryDto holdRule;

  @JsonProperty("StorageRule")
  private InheritedRuleCategoryDto storageRule;

  @JsonProperty("ReuseRule")
  private InheritedRuleCategoryDto reuseRule;

  @JsonProperty("ClassificationRule")
  private InheritedRuleCategoryDto classificationRule;

  @JsonProperty("DisseminationRule")
  private InheritedRuleCategoryDto disseminationRule;

  @JsonProperty("AccessRule")
  private InheritedRuleCategoryDto accessRule;
}
