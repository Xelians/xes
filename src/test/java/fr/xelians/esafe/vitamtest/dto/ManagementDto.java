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
public class ManagementDto {

  @JsonProperty("AppraisalRule")
  private RuleCategoryDto appraisalRule;

  @JsonProperty("AccessRule")
  private RuleCategoryDto accessRule;

  @JsonProperty("HoldRule")
  private RuleCategoryDto holdRule;
}
