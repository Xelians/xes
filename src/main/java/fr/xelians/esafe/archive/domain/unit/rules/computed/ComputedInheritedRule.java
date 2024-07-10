package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record ComputedInheritedRule(
    @JsonProperty("Rule") String ruleName, @JsonProperty("EndDate") LocalDate endDate) {}
