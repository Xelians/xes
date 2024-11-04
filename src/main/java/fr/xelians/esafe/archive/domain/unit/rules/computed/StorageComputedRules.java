/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.referential.domain.RuleType;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StorageComputedRules extends AbstractComputedRules implements FinalActionRule {

  @JsonProperty("FinalAction")
  protected List<String> finalActions = new ArrayList<>();

  public StorageComputedRules duplicate() {
    StorageComputedRules rules = new StorageComputedRules();
    rules.maxEndDate = this.maxEndDate;
    rules.inheritanceOrigin = this.inheritanceOrigin;
    rules.rules = new ArrayList<>(this.rules);
    rules.finalActions = this.finalActions;
    return rules;
  }

  @Override
  public RuleType getRuleType() {
    return RuleType.StorageRule;
  }

  // Vitam compatibility
  @Override
  @JsonIgnore
  public String getFinalAction() {
    return finalActions.isEmpty() ? null : finalActions.getFirst();
  }

  @Override
  @JsonIgnore
  public void setFinalAction(String finalAction) {
    finalActions.clear();
    if (StringUtils.isNotBlank(finalAction)) {
      finalActions.add(finalAction);
    }
  }
}
