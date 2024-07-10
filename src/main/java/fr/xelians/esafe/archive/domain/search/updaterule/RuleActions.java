/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RuleActions(
    @JsonProperty("add") @Size(max = 64) List<JsonNode> add,
    @JsonProperty("update") @Size(max = 64) List<JsonNode> update,
    @JsonProperty("delete") @Size(max = 64) List<JsonNode> delete) {}
