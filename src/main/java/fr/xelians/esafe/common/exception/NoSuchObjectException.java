/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception;

import java.io.IOException;

public class NoSuchObjectException extends IOException {

  public NoSuchObjectException() {
    super();
  }

  public NoSuchObjectException(String message) {
    super(message);
  }

  public NoSuchObjectException(Throwable cause) {
    super(cause);
  }

  public NoSuchObjectException(String message, Throwable cause) {
    super(message, cause);
  }
}
