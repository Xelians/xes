/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.computed;

import fr.xelians.esafe.referential.domain.RuleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReuseComputedRules extends AbstractComputedRules {

  public ReuseComputedRules duplicate() {
    ReuseComputedRules rules = new ReuseComputedRules();
    rules.maxEndDate = this.maxEndDate;
    return rules;
  }

  @Override
  public RuleType getRuleType() {
    return RuleType.ReuseRule;
  }
}
