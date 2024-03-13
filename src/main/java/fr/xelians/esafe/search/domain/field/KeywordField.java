/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

public class KeywordField extends Field {

  public static final String MAPPING_PREFIX = "Keyword";
  public static final String TYPE = "keyword";
  public static final int SIZE = 150;

  public KeywordField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public KeywordField(int value, boolean isStandard) {
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
    if (value == null) {
      throw new BadRequestException("Check type failed", "Failed to check null keyword value");
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public String asValue(JsonNode node) {
    if (!node.isTextual()) {
      throw new BadRequestException(
          "As value failed", String.format("Value '%s' is not a valid keyword", node.asText()));
    }
    return node.asText();
  }

  @Override
  public boolean isValid(String value) {
    return value != null;
  }
}
