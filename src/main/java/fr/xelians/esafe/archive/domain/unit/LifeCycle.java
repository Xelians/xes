/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationType;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
public record LifeCycle(
    @JsonProperty("_av") int autoVersion,
    @JsonProperty("_opi") long operationId,
    @JsonProperty("_opType") OperationType operationType,
    @JsonProperty("_opDate") LocalDateTime operationDate,
    @JsonProperty("_patch") String patch) {}
