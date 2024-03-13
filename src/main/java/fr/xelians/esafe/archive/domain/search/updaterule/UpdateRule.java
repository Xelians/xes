/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
