/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.dto.vitam;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.operation.domain.OperationState;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.vitam.StatusCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record VitamOperationDto(
    @JsonProperty("itemId") String itemId,
    @JsonProperty("message") String message,
    @JsonProperty("globalStatus") StatusCode globalStatus,
    @JsonProperty("globalState") OperationState globalState,
    @JsonProperty("globalOutcomeDetailSubcode") String globalOutcomeDetailSubcode,
    @JsonProperty("lifecycleEnable") boolean lifecycleEnable,
    @JsonProperty("statusMeter") List<Integer> statusMeter,
    @JsonProperty("data") Map<String, Object> data) {

  // Used in operationRepository
  public VitamOperationDto(
      Long id, String message, OperationStatus status, String outcome, boolean secured) {
    this(
        id.toString(),
        message,
        StatusCode.from(status),
        status.getState(),
        outcome,
        secured,
        new ArrayList<>(),
        new HashMap<>());
  }
}
