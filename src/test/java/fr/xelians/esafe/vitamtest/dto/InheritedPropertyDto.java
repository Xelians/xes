/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InheritedPropertyDto {

  /** Unit id */
  @JsonProperty("UnitId")
  private String unitId;

  /** Originating Agency Name */
  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  /** PropertyName */
  @JsonProperty("PropertyName")
  private String propertyName;

  /** PropertyValue */
  @JsonProperty("PropertyValue")
  private Object propertyValue;

  /** Paths */
  @JsonProperty("Paths")
  private List<ArrayList<String>> paths = new ArrayList<>();
}
