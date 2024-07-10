/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class FormatIdentification {

  @JsonProperty("FormatId")
  protected String formatId;

  @JsonProperty("FormatName")
  protected String formatName;

  @JsonProperty("FormatLitteral")
  protected String formatLitteral;

  @JsonProperty("MimeType")
  protected String mimeType;

  @JsonProperty("Encoding")
  protected String encoding;
}
