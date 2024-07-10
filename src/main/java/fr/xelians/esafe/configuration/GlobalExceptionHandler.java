/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import static fr.xelians.esafe.common.exception.Category.FUNCTIONAL;
import static fr.xelians.esafe.common.exception.Category.TECHNICAL;
import static fr.xelians.esafe.common.utils.ExceptionsUtils.format;
import static org.springframework.http.HttpStatus.*;

import fr.xelians.esafe.authentication.domain.AuthContext;
import fr.xelians.esafe.common.exception.Category;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.functional.ForbiddenException;
import fr.xelians.esafe.common.servlet.PbDetail;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
    String title = "Bad credentials error";
    PbDetail pb = createPbDetail(UNAUTHORIZED, title, ExceptionsUtils.getMessages(ex));
    log.warn(format(pb, ex.getMessage()), ex);
    return pb;
  }

  @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
  public ProblemDetail handleForbidden(ForbiddenException ex) {
    String title = "Access to resource is forbidden";
    PbDetail pb = createPbDetail(FORBIDDEN, title, ExceptionsUtils.getMessages(ex));
    log.warn(format(pb, ex.getMessage()), ex);
    return pb;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    String title = "Constraint violation error";
    PbDetail pb = createPbDetail(BAD_REQUEST, title, ExceptionsUtils.getMessages(ex));
    log.warn(format(pb, ex.getMessage()), ex);
    return pb;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String title = "Data Integrity violation error";
    PbDetail pb = createPbDetail(BAD_REQUEST, title, ExceptionsUtils.getMessages(ex));
    log.warn(format(pb, ex.getMessage()), ex);
    return pb;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleException(Exception ex) {
    PbDetail pb =
        (ex instanceof EsafeException e)
            ? createPbDetail(e)
            : createPbDetail(INTERNAL_SERVER_ERROR, ex.getClass().getSimpleName(), ex.getMessage());

    log(pb.getCategory(), format(pb, ex.getMessage()), ex);
    return pb;
  }

  @Override
  @NonNull
  protected ResponseEntity<Object> createResponseEntity(
      @Nullable Object body,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode statusCode,
      @NonNull WebRequest request) {

    // We avoid to log already logged problem detail
    if (!(body instanceof PbDetail)) {
      Category category = statusCode.is4xxClientError() ? FUNCTIONAL : TECHNICAL;
      String uri = createURI(request);
      String title = Objects.toString(body, "");
      String app = Objects.toString(getApp(), "");
      log(category, format(title, uri, statusCode, category, getTenant(), getUser(), app));
    }

    return new ResponseEntity<>(body, headers, statusCode);
  }

  @Override
  @NonNull
  protected ProblemDetail createProblemDetail(
      @NonNull Exception ex,
      @NonNull HttpStatusCode statusCode,
      @NonNull String detail,
      @Nullable String detailMessageCode,
      @Nullable Object[] detailMessageArguments,
      @NonNull WebRequest request) {

    PbDetail pb;

    if (ex instanceof EsafeException e) {
      pb = createPbDetail(e);
    } else {
      pb = createPbDetail(httpStatus(statusCode), detail, ex.getMessage());
    }
    log(pb.getCategory(), format(pb, ex.getMessage(), createURI(request)), ex);
    return pb;
  }

  private static PbDetail createPbDetail(EsafeException ex) {
    return PbDetail.builder()
        .status(ex.getHttpStatus())
        .title(ex.getTitle())
        .message(ex.getMessage())
        .type(ex.getType())
        .code(ex.getCode())
        .timestamp(ex.getTimestamp())
        .tenant(getTenant())
        .user(getUser())
        .app(getApp())
        .build();
  }

  private static PbDetail createPbDetail(HttpStatus status, String title, String message) {
    return PbDetail.builder()
        .status(status)
        .title(title)
        .message(message)
        .code(ExceptionsUtils.createCode())
        .timestamp(Instant.now())
        .tenant(getTenant())
        .user(getUser())
        .app(getApp())
        .build();
  }

  private static void log(Category category, String msg, Exception ex) {
    if (category == FUNCTIONAL) {
      log.warn(msg, ex);
    } else {
      log.error(msg, ex);
    }
  }

  private static void log(Category category, String msg) {
    if (category == FUNCTIONAL) {
      log.warn(msg);
    } else {
      log.error(msg);
    }
  }

  private static HttpStatus httpStatus(HttpStatusCode statusCode) {
    return HttpStatus.valueOf(statusCode.value());
  }

  private static String createURI(WebRequest request) {
    return request instanceof ServletWebRequest swr
        ? swr.getRequest().getMethod() + " " + swr.getRequest().getServletPath()
        : "";
  }

  private static String getTenant() {
    try {
      return AuthContext.getTenant().toString();
    } catch (Exception e) {
      return "";
    }
  }

  private static String getUser() {
    try {
      return AuthContext.getUserIdentifier();
    } catch (Exception e) {
      return "";
    }
  }

  private static String getApp() {
    try {
      return AuthContext.getApplicationId();
    } catch (Exception e) {
      return "";
    }
  }
}
