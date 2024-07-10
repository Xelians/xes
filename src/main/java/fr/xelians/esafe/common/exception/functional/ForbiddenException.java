/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception.functional;

import java.net.URI;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends FunctionalException {

  public ForbiddenException(String message) {
    super(message, URI.create(ABOUT_BLANK));
  }

  public ForbiddenException(String title, String message) {
    super(title, message, URI.create(ABOUT_BLANK));
  }

  public ForbiddenException(String title, String message, URI type) {
    super(title, message, type);
  }

  public ForbiddenException(String title, String message, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.FORBIDDEN;
  }
}
