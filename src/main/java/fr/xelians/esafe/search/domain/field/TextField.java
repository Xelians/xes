/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

public class TextField extends Field {

  public static final String MAPPING_PREFIX = "Text";
  public static final String TYPE = "text";
  public static final int SIZE = 200;

  public TextField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public TextField(int value, boolean isStandard) {
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
      throw new BadRequestException("Check type failed", "Failed to check null text value");
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public String asValue(JsonNode node) {
    if (!node.isTextual()) {
      throw new BadRequestException(
          "As value failed", String.format("Value '%s' is not a valid text", node.asText()));
    }
    return node.asText();
  }

  @Override
  public boolean isValid(String value) {
    return value != null;
  }
}
