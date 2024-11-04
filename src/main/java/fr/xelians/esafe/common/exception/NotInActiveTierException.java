/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception;

import java.io.IOException;

/*
 * @author Emmanuel Deviller
 */
public class NotInActiveTierException extends IOException {

  public NotInActiveTierException() {
    super();
  }

  public NotInActiveTierException(String message) {
    super(message);
  }

  public NotInActiveTierException(Throwable cause) {
    super(cause);
  }

  public NotInActiveTierException(String message, Throwable cause) {
    super(message, cause);
  }
}
