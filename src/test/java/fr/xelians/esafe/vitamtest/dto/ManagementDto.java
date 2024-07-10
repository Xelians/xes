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
public class ManagementDto {

  @JsonProperty("AppraisalRule")
  private RuleCategoryDto appraisalRule;

  @JsonProperty("AccessRule")
  private RuleCategoryDto accessRule;

  @JsonProperty("HoldRule")
  private RuleCategoryDto holdRule;
}
