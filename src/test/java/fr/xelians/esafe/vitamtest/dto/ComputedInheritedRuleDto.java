/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ComputedInheritedRuleDto {

  @JsonProperty("EndDates")
  private Map<String, String> endDatesByRuleCode;

  @JsonProperty("MaxEndDate")
  private String maxEndDate;

  @JsonProperty("InheritanceOrigin")
  private String inheritanceOrigin;

  @JsonProperty("InheritedRuleIds")
  private List<String> inheritedRuleIds;
}
