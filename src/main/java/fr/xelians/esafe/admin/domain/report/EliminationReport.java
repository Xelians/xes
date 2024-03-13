/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

public record EliminationReport(
    @NonNull @JsonProperty("Date") LocalDateTime date,
    @NonNull @JsonProperty("Tenant") Long tenant,
    @NonNull @JsonProperty("OperationId") Long operationId,
    @NonNull @JsonProperty("Type") ReportType type,
    @NonNull @JsonProperty("Status") ReportStatus status,
    @NonNull @JsonProperty("Units") List<Long> units) {}
