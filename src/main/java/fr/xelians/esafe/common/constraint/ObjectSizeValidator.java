/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
 * @author Emmanuel Deviller
 */
public class ObjectSizeValidator implements ConstraintValidator<ObjectSize, Sizable> {

  private long min;
  private long max;

  @Override
  public void initialize(ObjectSize constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    min = constraintAnnotation.min();
    max = constraintAnnotation.max();
  }

  @Override
  public boolean isValid(Sizable value, ConstraintValidatorContext context) {
    if (value == null) return true;
    long size = value.size();
    return size >= min && size <= max;
  }
}
