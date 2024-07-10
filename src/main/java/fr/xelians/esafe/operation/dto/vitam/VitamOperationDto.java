/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto.vitam;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.vitam.StatusCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record VitamOperationDto(
    @JsonProperty("itemId") String itemId,
    @JsonProperty("message") String message,
    @JsonProperty("globalStatus") StatusCode globalStatus,
    @JsonProperty("globalState") OperationState globalState,
    @JsonProperty("globalOutcomeDetailSubcode") String globalOutcomeDetailSubcode,
    @JsonProperty("lifecycleEnable") boolean lifecycleEnable,
    @JsonProperty("statusMeter") List<Integer> statusMeter,
    @JsonProperty("data") Map<String, Object> data) {

  // Used in operationRepository
  public VitamOperationDto(
      String id, String message, OperationStatus status, String outcome, boolean toSecure) {
    this(
        id,
        message,
        StatusCode.from(status),
        status.getState(),
        outcome,
        toSecure,
        new ArrayList<>(),
        new HashMap<>());
  }
}
