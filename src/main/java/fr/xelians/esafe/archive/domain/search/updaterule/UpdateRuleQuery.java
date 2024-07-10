/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import jakarta.validation.constraints.NotNull;

public record UpdateRuleQuery(
    @NotNull @JsonProperty("dslRequest") SearchQuery searchQuery,
    @NotNull @JsonProperty("ruleActions") RuleActions ruleActions) {}
