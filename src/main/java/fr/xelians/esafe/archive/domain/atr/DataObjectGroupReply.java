/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.atr;

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
public class DataObjectGroupReply {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  private String xmlId;

  @JsonProperty("PhysicalDataObjects")
  private List<PhysicalDataObjectReply> physicalDataObjectReplys = new ArrayList<>();

  @JsonProperty("BinaryDataObjects")
  private List<BinaryDataObjectReply> binaryDataObjectReplys = new ArrayList<>();
}
