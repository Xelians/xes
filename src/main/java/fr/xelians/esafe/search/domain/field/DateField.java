/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.DateUtils;
import java.time.LocalDateTime;

public class DateField extends Field {

  public static final String MAPPING_PREFIX = "Date";
  public static final String TYPE = "date";
  public static final int SIZE = 25;

  public DateField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public DateField(int value, boolean isStandard) {
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
    if (!DateUtils.isLocalDate(value) && !DateUtils.isLocalDateTime(value)) {
      throw new BadRequestException(
          "Check type failed", String.format("Value '%s' is not a valid date", value));
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public boolean isValid(String value) {
    return DateUtils.isLocalDate(value) || DateUtils.isLocalDateTime(value);
  }

  @Override
  public LocalDateTime asValue(JsonNode value) {
    return DateUtils.parseToLocalDateTime(value.asText());
  }
}
