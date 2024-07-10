/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InheritedProperty {

  @JsonProperty("UnitId")
  private String unitId;

  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  @JsonProperty("Paths")
  private List<String> paths = new ArrayList<>();

  @JsonProperty("PropertyName")
  private String propertyName;

  @JsonProperty("PropertyValue")
  private String propertyValue;
}
