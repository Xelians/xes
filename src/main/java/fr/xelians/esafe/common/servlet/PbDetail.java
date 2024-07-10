/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.servlet;

import static fr.xelians.esafe.common.exception.Category.FUNCTIONAL;
import static fr.xelians.esafe.common.exception.Category.TECHNICAL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.common.exception.Category;
import java.net.URI;
import java.time.Instant;
import lombok.Builder;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

public class PbDetail extends ProblemDetail {

  private static final String CODE = "Code";
  private static final String TIMESTAMP = "Timestamp";
  private static final String CATEGORY = "Category";
  private static final String TENANT = "Tenant";
  private static final String USER_ID = "UserId";
  private static final String APPLICATION_ID = "ApplicationId";

  @Builder
  public PbDetail(
      HttpStatusCode status,
      String title,
      String message,
      URI type,
      String code,
      Instant timestamp,
      URI instance,
      String tenant,
      String user,
      String app) {

    super(status.value());

    setTitle(title);
    setDetail(message);
    if (instance != null) setInstance(instance);
    if (type != null) setType(type);
    if (tenant != null) setProperty(TENANT, tenant);
    if (user != null) setProperty(USER_ID, user);
    if (app != null) setProperty(APPLICATION_ID, app);
    if (timestamp != null) setProperty(TIMESTAMP, timestamp);
    if (code != null) setProperty(CODE, code);
    setProperty(CATEGORY, status.is4xxClientError() ? FUNCTIONAL : TECHNICAL);
  }

  // Ignore these methods to be compatible with ProblemDetail on json serialization
  @JsonIgnore
  public String getTenant() {
    return getValue(TENANT);
  }

  @JsonIgnore
  public String getUserIdentifier() {
    return getValue(USER_ID);
  }

  @JsonIgnore
  public String getApplicationId() {
    return getValue(APPLICATION_ID);
  }

  @JsonIgnore
  public String getCode() {
    return getValue(CODE);
  }

  @JsonIgnore
  public Category getCategory() {
    String value = getValue(CATEGORY);
    return value == null ? null : Category.valueOf(value);
  }

  private String getValue(String key) {
    var properties = getProperties();
    if (properties == null) return "";
    Object value = properties.get(key);
    return value == null ? "" : value.toString();
  }
}
