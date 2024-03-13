/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.DurationRule;
import fr.xelians.esafe.archive.domain.unit.rules.FinalActionRule;
import fr.xelians.esafe.referential.domain.RuleType;
import lombok.*;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AppraisalRules extends AbstractSimpleRules implements FinalActionRule, DurationRule {

  @JsonProperty("FinalAction")
  protected String finalAction;

  // This property does not exist in SEDA
  @JsonProperty("Duration")
  protected String duration;

  @Override
  public RuleType getRuleType() {
    return RuleType.AppraisalRule;
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return super.isEmpty() && StringUtils.isBlank(finalAction);
  }
}
