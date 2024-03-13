/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PhysicalDataObject {

  //    @JsonIgnore
  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  @NonNull
  protected String xmlId;

  @JsonIgnore protected Long id;

  @NonNull
  @JsonProperty("PhysicalId")
  protected String physicalId;

  @JsonProperty("PhysicalVersion")
  protected String physicalVersion;

  @JsonProperty("Measure")
  protected Double measure;
}
