/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.inherited.InheritedRule;
import fr.xelians.esafe.common.utils.Utils;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Rule {

  @JsonProperty("Rule")
  protected String ruleName;

  @JsonProperty("StartDate")
  protected LocalDate startDate;

  @JsonProperty("EndDate")
  protected LocalDate endDate;

  public Rule(@JsonProperty("Rule") String ruleName) {
    Validate.notNull(ruleName, Utils.NOT_NULL, "ruleName");
    this.ruleName = ruleName;
  }

  @JsonCreator
  public Rule(
      @JsonProperty("Rule") String ruleName, @JsonProperty("StartDate") LocalDate startDate) {
    Validate.notNull(ruleName, Utils.NOT_NULL, "name");
    this.ruleName = ruleName;
    this.startDate = startDate;
  }

  @JsonIgnore
  public InheritedRule createInheritedRule() {
    InheritedRule inheritedRule = new InheritedRule();
    inheritedRule.setRule(ruleName);
    inheritedRule.setStartDate(startDate);
    inheritedRule.setEndDate(endDate);
    return inheritedRule;
  }
}
