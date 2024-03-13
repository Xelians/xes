/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.stream.Stream;

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
