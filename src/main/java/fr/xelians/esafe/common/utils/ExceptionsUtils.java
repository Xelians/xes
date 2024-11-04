/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.xelians.esafe.common.exception.Category;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.servlet.PbDetail;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.entity.OperationDb;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatusCode;

/*
 * @author Emmanuel Deviller
 */
public final class ExceptionsUtils {

  private ExceptionsUtils() {}

  public static final ObjectMapper MAPPER;

  static {
    MAPPER = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
  }

  public static String format(PbDetail pb, String message, String uri) {
    return String.format(
        "%s - %s - %s - %s - Category: %s - Code: %s - Tenant: %s - UserId: %s - ApplicationId: %s",
        HttpStatusCode.valueOf(pb.getStatus()),
        uri,
        pb.getTitle(),
        message,
        pb.getCategory(),
        pb.getCode(),
        pb.getTenant(),
        pb.getUserIdentifier(),
        Objects.toString(pb.getApplicationId(), ""));
  }

  public static String format(PbDetail pb, String message) {
    return String.format(
        "%s - %s - %s - Category: %s - Code: %s - Tenant: %s - UserId: %s - ApplicationId: %s",
        HttpStatusCode.valueOf(pb.getStatus()),
        pb.getTitle(),
        message,
        pb.getCategory(),
        pb.getCode(),
        pb.getTenant(),
        pb.getUserIdentifier(),
        Objects.toString(pb.getApplicationId(), ""));
  }

  public static String format(
      String title,
      String uri,
      HttpStatusCode statusCode,
      Category category,
      String tenant,
      String userIdentifier,
      String applicationId) {
    return String.format(
        "%s - %s - %s - Category: %s - Tenant: %s - UserId: %s - ApplicationId: %s",
        statusCode, uri, title, category, tenant, userIdentifier, applicationId);
  }

  public static String format(String title, EsafeException ex, OperationDb operation) {
    return format(
        title,
        ex.getText(),
        ex.getCategory(),
        ex.getCode(),
        operation.getType(),
        operation.getId(),
        operation.getTenant(),
        operation.getUserIdentifier(),
        operation.getApplicationId());
  }

  public static String format(EsafeException ex, OperationDb operation) {
    return format(
        ex.getTitle(),
        ex.getMessage(),
        ex.getCategory(),
        ex.getCode(),
        operation.getType(),
        operation.getId(),
        operation.getTenant(),
        operation.getUserIdentifier(),
        operation.getApplicationId());
  }

  public static String format(
      String title,
      String message,
      Category category,
      String code,
      OperationType operationType,
      Long operationId,
      Long tenant,
      String userIdentifier,
      String applicationId) {

    return String.format(
        "%s - OperationType: %s - OperationId: %s - Category: %s - Code: %s - Tenant: %s - UserId: %s - ApplicationId: %s",
        getText(title, message),
        operationType,
        operationId,
        category,
        code,
        tenant,
        userIdentifier,
        Objects.toString(applicationId, ""));
  }

  public static String getText(String title, String message) {
    if (StringUtils.isBlank(title)) return message;
    if (StringUtils.isBlank(message)) return title;
    return title + " - " + message;
  }

  public static String createCode() {
    return UUID.randomUUID().toString();
  }

  public static String getMessages(Throwable ex) {
    if (ex.getCause() == null) {
      return ex.getMessage();
    } else {
      String messages = getMessages(ex.getCause());
      return StringUtils.isBlank(messages) ? ex.getMessage() : messages + " - " + ex.getMessage();
    }
  }
}
