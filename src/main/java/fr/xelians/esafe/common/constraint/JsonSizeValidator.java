/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.utils.JsonUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JsonSizeValidator implements ConstraintValidator<JsonSize, JsonNode> {

  private long min;
  private long max;

  @Override
  public void initialize(JsonSize constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    min = constraintAnnotation.min();
    max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
    if (value == null) return true;
    long size = JsonUtils.calculateSize(value);
    return size >= min && size <= max;
  }
}
