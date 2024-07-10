/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BinaryDataObject {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  @NonNull
  protected String xmlId;

  @JsonProperty("_binaryId")
  protected Long id;

  @JsonProperty("_opi")
  protected Long operationId;

  @JsonProperty("_pos")
  protected long[] pos = {-1, -1};

  @JsonIgnore protected Path binaryPath;

  @JsonProperty("BinaryVersion")
  protected String binaryVersion;

  @JsonProperty("FormatIdentification")
  protected FormatIdentification formatIdentification;

  @NonNull
  @JsonProperty("MessageDigest")
  protected String messageDigest;

  @JsonProperty("Size")
  protected long size;

  @NonNull
  @JsonProperty("DigestAlgorithm")
  protected String digestAlgorithm = "SHA-512";

  @JsonProperty("FileInfo")
  protected FileInfo fileInfo;
}
