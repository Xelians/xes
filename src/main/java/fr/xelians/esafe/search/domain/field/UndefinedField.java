/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;

// The undefined field is typically used in index mapping to identify intermediate
// object fields without predefined type. This is useful for operators that access
// the field but don't need its type (as the 'exist' operator)
public class UndefinedField extends Field {

  public static final String TYPE = "Undefined";

  public UndefinedField(String value) {
    super(value, true);
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void check(String value) {
    throw new BadRequestException(
        "Check type value failed",
        String.format("Undefined field '%s' cannot check value '%s'", name, value));
  }

  @Override
  public boolean isValid(String value) {
    throw new BadRequestException(
        "is valid value failed", String.format("Undefined field '%s' cannot validate value", name));
  }

  @Override
  public Void asValue(JsonNode node) {
    throw new BadRequestException(
        "As value failed", String.format("Undefined field '%s' cannot return value", name));
  }
}
