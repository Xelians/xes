/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Utils;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class RuleInheritance {

  @JsonInclude
  @JsonProperty("PreventInheritance")
  protected Boolean preventInheritance = Boolean.FALSE;

  @NotNull
  @JsonInclude
  @JsonProperty("PreventRulesId")
  protected final List<String> preventRulesId = new ArrayList<>();

  public boolean addPreventRuleId(String ruleId) {
    Validate.notNull(ruleId, Utils.NOT_NULL, "ruleId");
    return preventRulesId.add(ruleId);
  }
}
