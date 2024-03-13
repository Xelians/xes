/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Text {

  @NonNull
  @JsonProperty("Message")
  protected final String message;

  @JsonProperty("Lang")
  protected final String lang;

  public Text(String message) {
    this(message, null);
  }

  @JsonCreator
  public Text(@JsonProperty("Message") @NonNull String message, @JsonProperty("Lang") String lang) {
    this.message = message;
    this.lang = lang;
  }
}
