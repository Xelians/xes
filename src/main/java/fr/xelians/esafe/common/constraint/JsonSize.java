/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.constraint;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = JsonSizeValidator.class)
public @interface JsonSize {

  /**
   * @return size the element must be greater or equal to
   */
  long min() default 0;

  /**
   * @return size the element must be lesser or equal to
   */
  long max() default 32_000;

  String message() default "Json size is out of range. Size must be between {min} and {max} bytes";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
