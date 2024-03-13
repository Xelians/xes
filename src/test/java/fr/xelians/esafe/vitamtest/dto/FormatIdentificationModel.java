/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FormatIdentificationModel {

  @JsonProperty("FormatLitteral")
  private String formatLitteral;

  @JsonProperty("MimeType")
  private String mimeType;

  @JsonProperty("FormatId")
  private String formatId;

  @JsonProperty("Encoding")
  private String encoding;
}
