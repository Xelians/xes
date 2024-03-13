/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

public class UndefinedField extends Field {

  public static final String TYPE = "";

  public UndefinedField(String value) {
    super(value, false);
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void check(String value) {
    throw new BadRequestException(
        "Check type failed", String.format("Undefined field cannot validate value '%s'", value));
  }

  @Override
  public boolean isValid(String value) {
    return false;
  }

  @Override
  public Void asValue(JsonNode node) {
    return null;
  }
}
