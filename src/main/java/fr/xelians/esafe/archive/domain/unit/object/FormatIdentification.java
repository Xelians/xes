/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class FormatIdentification {

  @JsonProperty("FormatId")
  protected String formatId;

  @JsonProperty("FormatName")
  protected String formatName;

  @JsonProperty("FormatLitteral")
  protected String formatLitteral;

  @JsonProperty("MimeType")
  protected String mimeType;

  @JsonProperty("Encoding")
  protected String encoding;
}
