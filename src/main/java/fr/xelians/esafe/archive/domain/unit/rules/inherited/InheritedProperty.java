/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.rules.inherited;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InheritedProperty {

  @JsonProperty("UnitId")
  private String unitId;

  @JsonProperty("OriginatingAgency")
  private String originatingAgency;

  @JsonProperty("Paths")
  private List<String> paths = new ArrayList<>();

  @JsonProperty("PropertyName")
  private String propertyName;

  @JsonProperty("PropertyValue")
  private String propertyValue;
}
