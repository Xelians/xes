/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InheritedHoldRule extends InheritedRule {

  @JsonProperty("HoldEndDate")
  protected LocalDate holdEndDate;

  @JsonProperty("HoldOwner")
  protected String holdOwner;

  @JsonProperty("HoldReason")
  protected String holdReason;

  @JsonProperty("HoldReassessingDate")
  protected LocalDate holdReassessingDate;

  @JsonProperty("PreventRearrangement")
  protected Boolean preventRearrangement;
}
