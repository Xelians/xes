/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.unit.DataObjectReference;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CustodialHistoryModelDto {

  @JsonProperty("CustodialHistoryItem")
  private List<String> custodialHistoryItem = new ArrayList<>();

  private DataObjectReference custodialHistoryFile;

  public CustodialHistoryModelDto() {}
}
