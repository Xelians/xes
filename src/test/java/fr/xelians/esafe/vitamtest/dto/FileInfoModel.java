/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileInfoModel {

  @JsonProperty("Filename")
  private String filename;

  @JsonProperty("CreatingApplicationName")
  private String creatingApplicationName;

  @JsonProperty("CreatingApplicationVersion")
  private String creatingApplicationVersion;

  @JsonProperty("CreatingOs")
  private String creatingOs;

  @JsonProperty("CreatingOsVersion")
  private String creatingOsVersion;

  @JsonProperty("LastModified")
  private String lastModified;

  @JsonProperty("DateCreatedByApplication")
  private String dateCreatedByApplication;
}
