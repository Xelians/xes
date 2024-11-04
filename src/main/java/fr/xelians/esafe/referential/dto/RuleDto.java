/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.referential.domain.RuleMeasurement;
import fr.xelians.esafe.referential.domain.RuleType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"Name", "Description"})
public class RuleDto extends AbstractReferentialDto {

  @NotNull
  @JsonProperty("RuleType")
  private RuleType type;

  @JsonProperty("RuleDuration")
  private String duration;

  @JsonProperty("RuleMeasurement")
  private RuleMeasurement measurement;

  // Maintain VITAM compatibility
  @JsonProperty("RuleDescription")
  @Override
  public void setDescription(String name) {
    this.description = name;
  }

  // Maintain VITAM compatibility
  @JsonProperty("RuleDescription")
  @Override
  public String getDescription() {
    return this.description;
  }

  // Maintain VITAM compatibility
  @JsonProperty("RuleValue")
  @Override
  public void setName(String name) {
    this.name = name;
  }

  // Maintain VITAM compatibility
  @JsonProperty("RuleValue")
  @Override
  public String getName() {
    return this.name;
  }

  @AssertTrue(message = "Duration or Measurement are not defined")
  @JsonIgnore
  private boolean validate() {
    return type == RuleType.HoldRule ? checkHoldRule() : checkStdRule();
  }

  @JsonIgnore
  private boolean checkHoldRule() {
    if (StringUtils.isBlank(duration)) {
      duration = "unlimited";
      measurement = RuleMeasurement.YEAR;
    }
    return measurement != null;
  }

  @JsonIgnore
  private boolean checkStdRule() {
    return StringUtils.isNotBlank(duration) && measurement != null;
  }
}
