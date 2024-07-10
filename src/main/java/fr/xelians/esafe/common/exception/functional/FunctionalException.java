/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception.functional;

import fr.xelians.esafe.common.exception.Category;
import fr.xelians.esafe.common.exception.EsafeException;
import java.net.URI;

public abstract class FunctionalException extends EsafeException {

  protected FunctionalException(String message, URI type) {
    super(message, type);
  }

  protected FunctionalException(String title, String message, URI type) {
    super(title, message, type);
  }

  protected FunctionalException(String title, String message, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public Category getCategory() {
    return Category.FUNCTIONAL;
  }
}
