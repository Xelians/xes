/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

public class BooleanField extends Field {

  public static final String MAPPING_PREFIX = "Boolean";
  public static final String TYPE = "boolean";
  public static final int SIZE = 10;

  public BooleanField(String value, boolean isStandard) {
    super(value, isStandard);
  }

  public BooleanField(int value, boolean isStandard) {
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
      Boolean.parseBoolean(value);
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          "Check type failed", String.format("Value '%s' is not a valid boolean number", value), e);
    }
  }

  public static String getFieldName(int value) {
    return getFieldName(MAPPING_PREFIX, value);
  }

  @Override
  public Boolean asValue(JsonNode node) {
    if (node.isBoolean()) {
      return node.asBoolean();
    }
    if (node.isTextual()) {
      try {
        return Boolean.parseBoolean(node.textValue());
      } catch (NumberFormatException ex) {
        // Do nothing here
      }
    }
    throw new BadRequestException(
        "As value failed", String.format("Value '%s' is not a valid boolean", node.asText()));
  }

  @Override
  public boolean isValid(String value) {
    try {
      Boolean.parseBoolean(value);
      return true;
    } catch (NumberFormatException ex) {
      return false;
    }
  }
}
