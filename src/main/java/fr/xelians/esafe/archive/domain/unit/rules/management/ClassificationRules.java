/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.referential.domain.RuleType;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ClassificationRules extends AbstractSimpleRules {

  @JsonProperty("ClassificationAudience")
  protected String classificationAudience;

  @JsonProperty("ClassificationLevel")
  protected String classificationLevel;

  @JsonProperty("ClassificationOwner")
  protected String classificationOwner;

  @JsonProperty("ClassificationReassessingDate")
  protected LocalDate classificationReassessingDate;

  @JsonProperty("NeedReassessingAuthorization")
  protected Boolean needReassessingAuthorization;

  @Override
  public RuleType getRuleType() {
    return RuleType.ClassificationRule;
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return super.isEmpty()
        && StringUtils.isBlank(classificationAudience)
        && StringUtils.isBlank(classificationLevel)
        && StringUtils.isBlank(classificationOwner)
        && classificationReassessingDate == null
        && needReassessingAuthorization == null;
  }
}
