/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.common.utils.Utils;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class AbstractSimpleRules extends AbstractRules {

  @NotNull
  @JsonProperty("Rules")
  protected final List<Rule> rules = new ArrayList<>();

  @Override
  @JsonIgnore
  public boolean addRule(Rule rule) {
    Validate.notNull(rule, Utils.NOT_NULL, "rule");
    return rules.add(rule);
  }

  @Override
  @JsonIgnore
  public boolean deleteRule(String ruleName) {
    Validate.notNull(ruleName, Utils.NOT_NULL, "ruleName");
    return rules.removeIf(rule -> Objects.equals(rule.getRuleName(), ruleName));
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return CollectionUtils.isEmpty(rules)
        && ruleInheritance.getPreventInheritance() != Boolean.TRUE;
  }
}
