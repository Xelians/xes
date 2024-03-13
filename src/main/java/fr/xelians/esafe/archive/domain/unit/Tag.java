/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Utils;
import org.apache.commons.lang3.Validate;

public record Tag(@JsonProperty("Key") String key, @JsonProperty("Value") String value) {

  @JsonCreator
  public Tag {
    Validate.notNull(value, Utils.NOT_NULL, "value");
  }
}
