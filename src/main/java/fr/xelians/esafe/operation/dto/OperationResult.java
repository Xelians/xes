/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.Hits;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record OperationResult<T>(
    @JsonProperty("httpCode") int httpCode,
    @JsonProperty("$hits") Hits hits,
    @JsonProperty("$results") List<T> results,
    @JsonProperty("$context") String context) {}
