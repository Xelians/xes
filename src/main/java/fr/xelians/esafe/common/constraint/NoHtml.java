/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constraint;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/*
 * @author Emmanuel Deviller
 */
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = NoHtmlValidator.class)
public @interface NoHtml {

  String message() default "Unsafe HTML tags included";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
