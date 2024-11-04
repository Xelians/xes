/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NonNull;

/*
 * @author Emmanuel Deviller
 */
public record RulesReport(
    @NonNull @JsonProperty("Type") ReportType type,
    @NonNull @JsonProperty("Date") LocalDateTime date,
    @NonNull @JsonProperty("Tenant") Long tenant,
    @NonNull @JsonProperty("Status") ReportStatus status,
    @NonNull @JsonProperty("insertedRules") List<String> insertedRules,
    @NonNull @JsonProperty("deletedRules") List<String> deletedRules,
    @NonNull @JsonProperty("operation") Operation operation) {

  public record Operation(
      @NonNull @JsonProperty("evId") String evId,
      @NonNull @JsonProperty("evDateTime") LocalDateTime evDateTime,
      @NonNull @JsonProperty("evType") String evType,
      @NonNull @JsonProperty("outMessg") String outMessg) {}
}
