/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.atr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ArchiveUnitReply {

  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  private String xmlId;

  @JsonProperty("SystemId")
  private String systemId;

  @JsonProperty("OriginatingSystemIds")
  private List<String> originatingSystemIds;

  @JsonProperty("ArchivalAgencyIdentifiers")
  private List<String> archivalAgencyIdentifiers;

  @JsonProperty("NumOfPhysicalObjects")
  Integer numOfPhysicalObjects;

  @JsonProperty("NumOfBinaryObjects")
  Integer numOfBinaryObjects;

  @JsonProperty("SizeOfBinaryObjects")
  private Long sizeOfBinaryObjects;

  @JsonIgnore
  public long getTotalObjects() {
    return (long) numOfPhysicalObjects + (long) numOfBinaryObjects;
  }
}
