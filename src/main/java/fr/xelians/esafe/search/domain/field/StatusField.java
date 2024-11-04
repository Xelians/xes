/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.referential.domain.Status;

/*
 * @author Emmanuel Deviller
 */
public class StatusField extends Field {

  public static final String TYPE = "status";

  public StatusField(String value, boolean isStandard) {
    super(value, isStandard);
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
      Status.valueOf(value);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Check type failed", String.format("Value '%s' is not a valid status", value), e);
    }
  }

  @Override
  public Status asValue(JsonNode node) {
    if (node.isTextual()) {
      try {
        return Status.valueOf(node.textValue());
      } catch (IllegalArgumentException ex) {
        // Do nothing here
      }
    }
    throw new BadRequestException(
        "As value failed", String.format("Value '%s' is not a valid status", node.asText()));
  }

  @Override
  public Status asValue(Object value) {
    if (value instanceof Status s) return s;
    return Status.valueOf(value.toString());
  }

  @Override
  public boolean isValid(String value) {
    return value != null;
  }
}
