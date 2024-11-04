/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import fr.xelians.esafe.referential.domain.RuleType;
import java.util.ArrayList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AccessComputedRules extends AbstractComputedRules {

  public AccessComputedRules duplicate() {
    AccessComputedRules rules = new AccessComputedRules();
    rules.maxEndDate = this.maxEndDate;
    rules.inheritanceOrigin = this.inheritanceOrigin;
    rules.rules = new ArrayList<>(this.rules);
    return rules;
  }

  @Override
  public RuleType getRuleType() {
    return RuleType.AccessRule;
  }
}
