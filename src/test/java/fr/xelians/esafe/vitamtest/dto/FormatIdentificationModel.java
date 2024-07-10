/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FormatIdentificationModel {

  @JsonProperty("FormatLitteral")
  private String formatLitteral;

  @JsonProperty("MimeType")
  private String mimeType;

  @JsonProperty("FormatId")
  private String formatId;

  @JsonProperty("Encoding")
  private String encoding;
}
