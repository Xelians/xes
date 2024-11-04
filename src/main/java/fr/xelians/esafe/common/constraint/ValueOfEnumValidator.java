/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Stream;

/*
 * @author Emmanuel Deviller
 */
public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, CharSequence> {

  private List<String> acceptedValues;

  @Override
  public void initialize(ValueOfEnum constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    acceptedValues =
        Stream.of(constraintAnnotation.enumClass().getEnumConstants()).map(Enum::name).toList();
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || acceptedValues.contains(value.toString());
  }
}
