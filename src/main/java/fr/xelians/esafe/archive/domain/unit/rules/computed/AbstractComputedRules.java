/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.referential.domain.RuleType;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class AbstractComputedRules {

  @JsonProperty("MaxEndDate")
  protected LocalDate maxEndDate;

  @JsonIgnore
  public abstract AbstractComputedRules duplicate();

  @JsonIgnore
  public abstract RuleType getRuleType();
}
