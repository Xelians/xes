/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver.expression;

import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

/*
 * @author Youcef Bouhaddouza
 */
@Getter
@Setter
public class MethodSecurityExpressionRoot extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {
  private Object filterObject;
  private Object returnObject;
  private Object target;

  public MethodSecurityExpressionRoot(Supplier<Authentication> authentication) {
    super(authentication);
  }

  public void setThis(Object target) {
    this.target = target;
  }

  public Object getThis() {
    return this.target;
  }
}
