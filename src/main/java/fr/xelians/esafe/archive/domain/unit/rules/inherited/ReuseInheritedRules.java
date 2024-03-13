/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import fr.xelians.esafe.referential.domain.RuleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReuseInheritedRules extends AbstractInheritedRules {

  @Override
  public RuleType getRuleType() {
    return RuleType.ReuseRule;
  }
}
