/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {
  //    private static final PolicyFactory DISALLOW_ALL = new HtmlPolicyBuilder().toFactory();

  @Override
  public void initialize(NoHtml constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    //        return value == null || DISALLOW_ALL.sanitize(value).equals(value);
    return value == null || !StringUtils.containsAny(value, "<", ">");
  }
}
