/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.Rule;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.referential.domain.RuleType;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.Validate;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HoldRules extends AbstractRules {

  @NotNull
  @JsonProperty("Rules")
  protected final List<HoldRule> rules = new ArrayList<>();

  @Override
  @JsonIgnore
  public boolean addRule(Rule rule) {
    Validate.notNull(rule, Utils.NOT_NULL, "rule");
    if (rule instanceof HoldRule holdRule) {
      return rules.add(holdRule);
    } else {
      throw new InternalException(
          "Fail to add hold rule",
          String.format("%s is not a valid hold rule", rule.getRuleName()));
    }
  }

  @JsonIgnore
  public boolean addRule(HoldRule holdRule) {
    Validate.notNull(holdRule, Utils.NOT_NULL, "holdRule");
    return rules.add(holdRule);
  }

  @Override
  @JsonIgnore
  public boolean deleteRule(String ruleName) {
    Validate.notNull(ruleName, Utils.NOT_NULL, "ruleName");
    return rules.removeIf(rule -> Objects.equals(rule.getRuleName(), ruleName));
  }

  @Override
  @JsonIgnore
  public boolean isEmpty() {
    return CollectionUtils.isEmpty(rules)
        && ruleInheritance.getPreventInheritance() != Boolean.TRUE;
  }

  @Override
  public RuleType getRuleType() {
    return RuleType.HoldRule;
  }

  public List<Rule> getRules() {
    return rules.stream().map(r -> (Rule) r).toList();
  }

  @JsonIgnore
  public List<HoldRule> getHoldRules() {
    return rules;
  }

  @JsonIgnore
  public Boolean getPreventRearrangement() {
    for (HoldRule rule : rules) {
      if (BooleanUtils.isTrue(rule.preventRearrangement)) return true;
    }
    return false;
  }
}
