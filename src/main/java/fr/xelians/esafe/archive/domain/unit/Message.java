/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class Message {

  @JsonProperty("Date")
  protected LocalDateTime date;

  @JsonProperty("MessageIdentifier")
  protected String messageIdentifier;

  @JsonProperty("Comment")
  protected String comment;
}
