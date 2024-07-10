/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

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
public class DataObjectGroup {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  protected String xmlId;

  @JsonProperty("PhysicalDataObjects")
  protected List<PhysicalDataObject> physicalDataObjects = new ArrayList<>();

  @JsonProperty("BinaryDataObjects")
  protected List<BinaryDataObject> binaryDataObjects = new ArrayList<>();

  public void addPhysicalDataObject(PhysicalDataObject physicalDataObject) {
    physicalDataObjects.add(physicalDataObject);
  }

  public void addBinaryDataObject(BinaryDataObject binaryDataObject) {
    binaryDataObjects.add(binaryDataObject);
  }
}
