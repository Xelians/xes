/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class UpdateOperation {

  @JsonProperty("SystemId")
  private String systemId;

  @JsonProperty("MetadataName")
  private String metadataName;

  @JsonProperty("MetadataValue")
  private String metadataValue;
}
