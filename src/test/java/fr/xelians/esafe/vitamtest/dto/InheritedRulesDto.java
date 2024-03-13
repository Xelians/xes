/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InheritedRulesDto {

  @JsonProperty("AppraisalRule")
  private InheritedRuleCategoryDto appraisalRule;

  @JsonProperty("HoldRule")
  private InheritedRuleCategoryDto holdRule;

  @JsonProperty("StorageRule")
  private InheritedRuleCategoryDto storageRule;

  @JsonProperty("ReuseRule")
  private InheritedRuleCategoryDto reuseRule;

  @JsonProperty("ClassificationRule")
  private InheritedRuleCategoryDto classificationRule;

  @JsonProperty("DisseminationRule")
  private InheritedRuleCategoryDto disseminationRule;

  @JsonProperty("AccessRule")
  private InheritedRuleCategoryDto accessRule;
}
