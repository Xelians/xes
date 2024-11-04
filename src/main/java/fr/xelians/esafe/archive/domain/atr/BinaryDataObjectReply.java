/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.atr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class BinaryDataObjectReply {

  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  private String xmlId;

  @JsonProperty("DataObjectSystemId")
  private Long systemId;

  @JsonProperty("DataObjectVersion")
  private String version;

  @JsonProperty("Size")
  private Long size;

  @JsonProperty("MessageDigest")
  protected String messageDigest;

  @JsonProperty("DigestAlgorithm")
  protected String digestAlgorithm;
}
