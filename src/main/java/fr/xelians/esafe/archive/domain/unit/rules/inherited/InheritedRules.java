/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.rules.computed.*;
import fr.xelians.esafe.archive.domain.unit.rules.management.*;
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
public class InheritedRules {

  @JsonProperty("GlobalProperties")
  @JsonInclude
  protected final List<InheritedProperty> globalProperties = new ArrayList<>();

  @JsonProperty("AccessRule")
  protected AccessInheritedRules accessRules;

  @JsonProperty("AppraisalRule")
  protected AppraisalInheritedRules appraisalRules;

  @JsonProperty("DisseminationRule")
  protected DisseminationInheritedRules disseminationRules;

  @JsonProperty("ReuseRule")
  protected ReuseInheritedRules reuseRules;

  @JsonProperty("ClassificationRule")
  protected ClassificationInheritedRules classificationRules;

  @JsonProperty("StorageRule")
  protected StorageInheritedRules storageRules;

  @JsonProperty("HoldRule")
  protected HoldInheritedRules holdRules;

  @JsonIgnore
  public AbstractInheritedRules getRules(RuleType ruleType) {
    return switch (ruleType) {
      case ReuseRule -> reuseRules;
      case AccessRule -> accessRules;
      case AppraisalRule -> appraisalRules;
      case ClassificationRule -> classificationRules;
      case StorageRule -> storageRules;
      case DisseminationRule -> disseminationRules;
      case HoldRule -> holdRules;
    };
  }
}
