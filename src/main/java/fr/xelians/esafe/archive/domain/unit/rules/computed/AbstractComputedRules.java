/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@EqualsAndHashCode
@ToString
public abstract class AbstractComputedRules {

  @JsonProperty("MaxEndDate")
  protected LocalDate maxEndDate;

  @JsonProperty("InheritanceOrigin")
  protected InheritanceOrigin inheritanceOrigin = InheritanceOrigin.LOCAL;

  @JsonProperty("Rules")
  protected List<ComputedInheritedRule> rules = new ArrayList<>();

  @JsonIgnore
  public abstract AbstractComputedRules duplicate();

  @JsonIgnore
  public abstract RuleType getRuleType();
}
