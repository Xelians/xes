/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

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
