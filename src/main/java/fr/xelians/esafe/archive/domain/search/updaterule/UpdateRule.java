/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public record UpdateRule(
    @JsonProperty("OldRule") String oldRule,
    @JsonProperty("Rule") String rule,
    @JsonProperty("DeleteStartDate") Boolean deleteStartDate,
    @JsonProperty("StartDate") LocalDate startDate,
    @JsonProperty("DeleteHoldEndDate") Boolean deleteHoldEndDate,
    @JsonProperty("HoldEndDate") LocalDate holdEndDate,
    @JsonProperty("DeleteHoldOwner") Boolean deleteHoldOwner,
    @JsonProperty("HoldOwner") String holdOwner,
    @JsonProperty("DeleteHoldReason") Boolean deleteHoldReason,
    @JsonProperty("HoldReason") String holdReason,
    @JsonProperty("DeleteHoldReassessingDate") Boolean deleteHoldReassessingDate,
    @JsonProperty("HoldReassessingDate") LocalDate holdReassessingDate,
    @JsonProperty("DeletePreventRearrangement") Boolean deletePreventRearrangement,
    @JsonProperty("PreventRearrangement") Boolean preventRearrangement) {}
