/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import org.apache.commons.lang3.math.NumberUtils;

public class DoubleField extends Field {

  public static final String MAPPING_PREFIX = "Double";
  public static final String TYPE = "double";
  public static final int SIZE = 20;

  public DoubleField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public DoubleField(int value, boolean isStandard) {
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
      Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          "Check type failed", String.format("Value '%s' is not a valid double", value), e);
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public Double asValue(JsonNode node) {
    if (node.isNumber()) {
      return node.asDouble();
    }
    if (node.isTextual()) {
      try {
        return Double.parseDouble(node.textValue());
      } catch (NumberFormatException ex) {
        // Do nothing here
      }
    }
    throw new BadRequestException(
        "As value failed", String.format("Value '%s' is not a valid double number", node.asText()));
  }

  @Override
  public boolean isValid(String value) {
    return NumberUtils.isParsable(value);
  }
}
