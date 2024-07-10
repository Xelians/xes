/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.exception;

import fr.xelians.esafe.common.utils.ExceptionsUtils;
import java.net.URI;
import java.time.Instant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A specialized Exception
 *
 * @author Emmanuel Deviller
 */
@Getter
public abstract class EsafeException extends RuntimeException {

  public static final String ABOUT_BLANK = "about:blank";

  private final String title;
  private final URI type;
  private final String code;
  private final Instant timestamp = Instant.now();

  protected EsafeException(String message, URI type) {
    this("", message, type);
  }

  protected EsafeException(String title, String message, URI type) {
    super(message);
    this.title = title;
    this.type = type;
    this.code = ExceptionsUtils.createCode();
  }

  protected EsafeException(String title, String message, URI type, Throwable cause) {
    super(message, cause);
    this.title = title;
    this.type = type;
    this.code = ExceptionsUtils.createCode();
  }

  public abstract HttpStatus getHttpStatus();

  public abstract Category getCategory();

  public String getText() {
    return ExceptionsUtils.getText(title, getMessage());
  }

  public String getTexts() {
    return ExceptionsUtils.getText(title, ExceptionsUtils.getMessages(this));
  }
}
