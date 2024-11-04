/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/*
 * @author Emmanuel Deviller
 */
@Data
public class LogbookEventDto {

  private String evId;
  private String evParentId;
  private String evType;
  private String evDateTime;
  private String evIdProc;
  private String evTypeProc;
  private String outcome;
  private String outDetail;
  private String outMessg;
  private String agId;
  private String obId;
  private String evDetData;
  private String rightsStatementIdentifier;

  @JsonProperty("_lastPersistedDate")
  private String lastPersistedDate;
}
