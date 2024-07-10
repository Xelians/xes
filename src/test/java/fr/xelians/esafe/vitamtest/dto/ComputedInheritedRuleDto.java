/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ComputedInheritedRuleDto {

  @JsonProperty("EndDates")
  private Map<String, String> endDatesByRuleCode;

  @JsonProperty("MaxEndDate")
  private String maxEndDate;

  @JsonProperty("InheritanceOrigin")
  private String inheritanceOrigin;

  @JsonProperty("InheritedRuleIds")
  private List<String> inheritedRuleIds;
}
