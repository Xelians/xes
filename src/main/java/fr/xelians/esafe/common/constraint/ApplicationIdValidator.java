/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

/*
 * @author Emmanuel Deviller
 */
public class ApplicationIdValidator implements ConstraintValidator<NoHtml, String> {

  @Override
  public void initialize(NoHtml constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    if (value == null) return true;

    if (value.length() > Header.X_APPLICATION_LEN) {
      String message =
          String.format(
              "Header %s is too long (%d characters max)",
              X_APPLICATION_ID, Header.X_APPLICATION_LEN);
      ((ConstraintValidatorContextImpl) context).addMessageParameter(message, value);
      return false;
    }

    if (Utils.isNotHtmlSafe(value)) {
      String message = String.format("Header %s is not html safe", X_APPLICATION_ID);
      ((ConstraintValidatorContextImpl) context).addMessageParameter(message, value);
      return false;
    }

    if (Utils.containsNotAllowedChar(value)) {
      String message = String.format("Header %s contains not allowed chars", X_APPLICATION_ID);
      ((ConstraintValidatorContextImpl) context).addMessageParameter(message, value);
      return false;
    }

    return true;
  }
}
