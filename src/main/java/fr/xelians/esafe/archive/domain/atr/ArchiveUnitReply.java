/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.atr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ArchiveUnitReply {

  @JsonProperty(value = "XmlId", access = JsonProperty.Access.WRITE_ONLY)
  private String xmlId;

  @JsonProperty("SystemId")
  private String systemId;

  @JsonProperty("OriginatingSystemId")
  private String originatingSystemId;
}
