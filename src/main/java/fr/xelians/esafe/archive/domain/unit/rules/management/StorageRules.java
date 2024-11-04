/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.referential.domain.RuleType;
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
public class StorageRules extends AbstractSimpleRules implements FinalActionRule {

  @JsonProperty("FinalAction")
  protected String finalAction;

  @Override
  public RuleType getRuleType() {
    return RuleType.StorageRule;
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return super.isEmpty() && StringUtils.isBlank(finalAction);
  }
}
