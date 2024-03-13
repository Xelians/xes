/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

public class LongField extends Field {

  public static final String MAPPING_PREFIX = "Long";
  public static final String TYPE = "long";
  public static final int SIZE = 20;

  public LongField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public LongField(int value, boolean isStandard) {
    super(getFieldName(value), isStandard);
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void check(String value) {
    checkType(value);
  }

  public static void checkType(String value) {
    try {
      Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          "Check type failed", String.format("Value '%s' is not a valid long number", value), e);
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public Long asValue(JsonNode node) {
    if (node.isLong() || node.isInt()) {
      return node.asLong();
    }
    if (node.isTextual()) {
      try {
        return Long.parseLong(node.textValue());
      } catch (NumberFormatException ex) {
        // Do nothing here
      }
    }
    throw new BadRequestException(
        "As value failed", String.format("Value '%s' is not a valid long number", node.asText()));
  }

  @Override
  public boolean isValid(String value) {
    try {
      Long.parseLong(value);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }
}
