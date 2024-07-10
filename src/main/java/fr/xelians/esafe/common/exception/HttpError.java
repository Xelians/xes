/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception;

import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class HttpError {

  private final long timestamp;
  private final HttpStatus status;
  private final String name;
  private final String message;
  private final List<String> errors;

  public HttpError(HttpStatus status, String message, String name, String error) {
    this(status, message, name, List.of(error));
  }

  public HttpError(HttpStatus status, String message, String name, List<String> errors) {
    this.timestamp = System.currentTimeMillis();
    this.status = status;
    this.name = name;
    this.message = message;
    this.errors = errors;
  }
}
