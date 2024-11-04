/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InheritedHoldRule extends InheritedRule {

  @JsonProperty("HoldEndDate")
  protected LocalDate holdEndDate;

  @JsonProperty("HoldOwner")
  protected String holdOwner;

  @JsonProperty("HoldReason")
  protected String holdReason;

  @JsonProperty("HoldReassessingDate")
  protected LocalDate holdReassessingDate;

  @JsonProperty("PreventRearrangement")
  protected Boolean preventRearrangement;
}
