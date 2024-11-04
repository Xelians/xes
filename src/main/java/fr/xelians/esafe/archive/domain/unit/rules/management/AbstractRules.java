/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.referential.domain.RuleType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class AbstractRules {

  @NotNull
  @JsonProperty("Inheritance")
  protected final RuleInheritance ruleInheritance = new RuleInheritance();

  @JsonProperty("EndDate")
  protected LocalDate endDate;

  @JsonProperty("Rules")
  public abstract List<Rule> getRules();

  @JsonIgnore
  public abstract boolean addRule(Rule rule);

  @JsonIgnore
  public abstract boolean deleteRule(String ruleName);

  @JsonIgnore
  public abstract RuleType getRuleType();

  @JsonIgnore
  public abstract boolean isEmpty();
}
