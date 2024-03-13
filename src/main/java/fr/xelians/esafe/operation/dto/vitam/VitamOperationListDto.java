/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto.vitam;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.vitam.StatusCode;
import fr.xelians.esafe.operation.dto.OperationDto;
import java.time.LocalDateTime;

public record VitamOperationListDto(
    @JsonProperty("operationId") String operationId,
    @JsonProperty("processType") OperationType operationType,
    @JsonProperty("globalState") OperationState globalState,
    @JsonProperty("previousStep") String previousStep,
    @JsonProperty("nextStep") String nextStep,
    @JsonProperty("processDate") LocalDateTime processDate,
    @JsonProperty("stepByStep") boolean stepByStep,
    @JsonProperty("stepStatus") StatusCode stepStatus) {

  public VitamOperationListDto(OperationDto ope) {
    this(
        ope.id().toString(),
        ope.type(),
        ope.status().getState(),
        "",
        "",
        ope.created(),
        false,
        StatusCode.from(ope.status()));
  }
}
