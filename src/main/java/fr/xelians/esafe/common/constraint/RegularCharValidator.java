/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import fr.xelians.esafe.common.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
 * @author Emmanuel Deviller
 */
public class RegularCharValidator implements ConstraintValidator<RegularChar, String> {

  @Override
  public void initialize(RegularChar constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    return value == null || !Utils.containsNotAllowedChar(value);
  }
}
