/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception.technical;

import java.net.URI;
import org.springframework.http.HttpStatus;

/*
 * @author Emmanuel Deviller
 */
public class TimeOutException extends TechnicalException {

  public TimeOutException(String title, String message) {
    super(title, message, URI.create(ABOUT_BLANK));
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.GATEWAY_TIMEOUT;
  }
}
