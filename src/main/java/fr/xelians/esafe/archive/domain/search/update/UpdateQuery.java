/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.constraint.JsonSize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record UpdateQuery(
    @JsonProperty("$roots") @Size(max = 1024) List<Long> roots,
    @JsonProperty("$type") @Size(max = 1024) String type,
    @NotNull @JsonProperty("$query") @JsonSize JsonNode queryNode,
    @JsonProperty("$threshold") Long threshold,
    @JsonProperty("$filter") @JsonSize JsonNode filterNode,
    @NotNull @JsonProperty("$action") @JsonSize JsonNode actionNode) {}
