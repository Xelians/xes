/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.DateUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
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

  @Override
  public LocalDateTime asValue(Object value) {
    if (value instanceof LocalDateTime ldt) return ldt;
    if (value instanceof LocalDate ld) return ld.atStartOfDay();
    return DateUtils.parseToLocalDateTime(value.toString());
  }
}
