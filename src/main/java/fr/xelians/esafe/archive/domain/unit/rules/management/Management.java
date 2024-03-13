/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.referential.domain.RuleType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Management {

  @JsonProperty("OriginatingAgencyIdentifier")
  protected String originatingAgencyIdentifier;

  @JsonProperty("SubmissionAgencyIdentifier")
  protected String submissionAgencyIdentifier;

  @JsonProperty("AccessRule")
  protected AccessRules accessRules;

  @JsonProperty("AppraisalRule")
  protected AppraisalRules appraisalRules;

  @JsonProperty("DisseminationRule")
  protected DisseminationRules disseminationRules;

  @JsonProperty("ReuseRule")
  protected ReuseRules reuseRules;

  @JsonProperty("ClassificationRule")
  protected ClassificationRules classificationRules;

  @JsonProperty("StorageRule")
  protected StorageRules storageRules;

  @JsonProperty("HoldRule")
  protected HoldRules holdRules;

  @JsonIgnore
  public boolean hasRules() {
    return accessRules != null
        || appraisalRules != null
        || disseminationRules != null
        || reuseRules != null
        || classificationRules != null
        || storageRules != null
        || holdRules != null;
  }

  @JsonIgnore
  public void setRules(AbstractRules abstractRules) {
    switch (abstractRules) {
      case AppraisalRules rule -> this.appraisalRules = rule;
      case AccessRules rule -> this.accessRules = rule;
      case DisseminationRules rule -> this.disseminationRules = rule;
      case ReuseRules rule -> this.reuseRules = rule;
      case ClassificationRules rule -> this.classificationRules = rule;
      case StorageRules rule -> this.storageRules = rule;
      case HoldRules rule -> this.holdRules = rule;
      default -> throw new InternalException(
          String.format("Bad abstractRule: %s", abstractRules.getClass().getSimpleName()));
    }
  }

  @JsonIgnore
  public AbstractRules getRules(RuleType ruleType) {
    return switch (ruleType) {
      case ReuseRule -> reuseRules;
      case AccessRule -> accessRules;
      case AppraisalRule -> appraisalRules;
      case ClassificationRule -> classificationRules;
      case HoldRule -> holdRules;
      case StorageRule -> storageRules;
      case DisseminationRule -> disseminationRules;
    };
  }
}
