/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constraint;

import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.utils.Utils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;

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
