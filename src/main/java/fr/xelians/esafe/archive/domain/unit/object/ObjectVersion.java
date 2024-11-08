/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.List;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ObjectVersion {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  @NonNull
  protected String xmlId;

  @NonNull
  @JsonProperty("_id")
  protected Long id;

  @JsonProperty("PhysicalId")
  protected String physicalId;

  @JsonProperty("Measure")
  protected Double measure;

  @JsonProperty("DataObjectVersion")
  protected String dataObjectVersion;

  @JsonProperty("_pos")
  protected long[] pos;

  @JsonIgnore protected Path binaryPath;

  @JsonProperty("FormatIdentification")
  protected FormatIdentification formatIdentification;

  @JsonProperty("MessageDigest")
  protected String messageDigest;

  @JsonProperty("Size")
  protected long size;

  @JsonProperty("Algorithm")
  protected String algorithm;

  @JsonProperty("FileInfo")
  protected FileInfo fileInfo;

  @JsonProperty("_opi")
  protected Long operationId;

  @JsonIgnore
  public static ObjectVersion getGreatestVersion(List<ObjectVersion> versions) {
    int max = -1;
    ObjectVersion maxVersion = null;

    for (ObjectVersion version : versions) {
      String[] tks = StringUtils.split(version.getDataObjectVersion(), '_');
      int v = Integer.parseInt(tks[1]);
      if (v > max) {
        max = v;
        maxVersion = version;
      }
    }
    return maxVersion;
  }

  @JsonIgnore
  public static ObjectVersion getVersion(List<ObjectVersion> versions, BinaryVersion version) {
    String bv = version.toString();
    for (ObjectVersion ov : versions) {
      if (bv.equals(ov.getDataObjectVersion())) {
        return ov;
      }
    }
    return null;
  }

  @JsonIgnore
  public String getFilename() {
    return fileInfo == null ? null : fileInfo.getFilename();
  }

  @JsonIgnore
  public String getMimeType() {
    return formatIdentification == null ? null : formatIdentification.getMimeType();
  }
}
