/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/*
 * @author Emmanuel Deviller
 */
public record ComputedInheritedRule(
    @JsonProperty("Rule") String ruleName, @JsonProperty("EndDate") LocalDate endDate) {}
