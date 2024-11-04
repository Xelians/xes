/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Hash;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class AtrDetail {

  @NonNull
  @JsonProperty("OperationId")
  private Long operationId;

  @NonNull
  @JsonProperty("Algorithm")
  private Hash algorithm;

  @NonNull
  @JsonProperty("StorageAtrChecksum")
  private String storageAtrChecksum;

  @NonNull
  @JsonProperty("LogbookAtrChecksum")
  private String lbkAtrChecksum;

  @NonNull
  @JsonProperty("Status")
  private ReportStatus status;

  @JsonProperty("StatusDetail")
  private String statusDetail;
}
