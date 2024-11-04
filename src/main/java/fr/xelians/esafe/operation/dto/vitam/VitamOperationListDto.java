/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto.vitam;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.vitam.StatusCode;
import fr.xelians.esafe.operation.dto.OperationDto;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
public record VitamOperationListDto(
    @JsonProperty("operationId") String operationId,
    @JsonProperty("processType") OperationType operationType,
    @JsonProperty("globalState") OperationState globalState,
    @JsonProperty("previousStep") String previousStep,
    @JsonProperty("nextStep") String nextStep,
    @JsonProperty("processDate") LocalDateTime processDate,
    @JsonProperty("stepByStep") boolean stepByStep,
    @JsonProperty("stepStatus") StatusCode stepStatus) {

  public VitamOperationListDto(OperationDto ope) {
    this(
        ope.id(),
        ope.type(),
        ope.status().getState(),
        "",
        "",
        ope.created(),
        false,
        StatusCode.from(ope.status()));
  }
}
