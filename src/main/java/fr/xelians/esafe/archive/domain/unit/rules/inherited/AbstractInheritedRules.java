/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.referential.domain.RuleType;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public abstract class AbstractInheritedRules {

  @JsonProperty("Rules")
  @JsonInclude
  protected final List<InheritedRule> rules = new ArrayList<>();

  @JsonProperty("Properties")
  @JsonInclude
  protected final List<InheritedProperty> properties = new ArrayList<>();

  @JsonIgnore
  public abstract RuleType getRuleType();
}
