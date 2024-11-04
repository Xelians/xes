/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProbativeValueReport {

  @NonNull
  @JsonProperty("Date")
  private LocalDateTime date;

  @NonNull
  @JsonProperty("Tenant")
  private Long tenant;

  @NonNull
  @JsonProperty("OperationId")
  private Long operationId;

  @NonNull
  @JsonProperty("Type")
  private ReportType type;

  @NonNull
  @JsonProperty("Status")
  ReportStatus status;

  @NonNull
  @JsonProperty("BinaryObjectDetail")
  private List<BinaryObjectDetail> binaryObjectDetails = new ArrayList<>();

  @NonNull
  @JsonProperty("AtrDetail")
  private List<AtrDetail> atrDetails = new ArrayList<>();
}
