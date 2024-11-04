/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PhysicalDataObject {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  @NonNull
  protected String xmlId;

  @JsonIgnore protected Long id;

  @NonNull
  @JsonProperty("PhysicalId")
  protected String physicalId;

  @JsonProperty("PhysicalVersion")
  protected String physicalVersion;

  @JsonProperty("Measure")
  protected Double measure;
}
