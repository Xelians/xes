/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constraint;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

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
