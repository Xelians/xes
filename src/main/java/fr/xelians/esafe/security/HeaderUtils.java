/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.Utils;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public final class HeaderUtils {

  public static final String BAD_HEADER = "Bad header";

  private HeaderUtils() {}

  public static Long getTenant(String tenantIdHeader) {
    if (StringUtils.isBlank(tenantIdHeader)) {
      return null;
    }

    long tenant;
    try {
      tenant = Long.parseLong(tenantIdHeader);
    } catch (NumberFormatException e) {
      throw new BadRequestException(
          BAD_HEADER,
          "%s header is not a number: %s".formatted(Header.X_TENANT_ID, tenantIdHeader));
    }

    if (tenant < 0) {
      throw new BadRequestException(
          BAD_HEADER, "%s header cannot be negative: %d".formatted(Header.X_TENANT_ID, tenant));
    }

    return tenant;
  }

  public static String getApplicationId(String appIdHeader) {
    if (StringUtils.isBlank(appIdHeader)) return "";

    if (appIdHeader.length() > Header.X_APPLICATION_LEN) {
      throw new BadRequestException(
          BAD_HEADER,
          "X_APPLICATION_ID header %s is too long (%d characters max)"
              .formatted(appIdHeader, Header.X_APPLICATION_LEN));
    }

    if (Utils.isNotHtmlSafe(appIdHeader)) {
      throw new BadRequestException(
          BAD_HEADER, "X_APPLICATION_ID header %s is not html safe".formatted(appIdHeader));
    }

    if (Utils.containsNotAllowedChar(appIdHeader)) {
      throw new BadRequestException(
          BAD_HEADER,
          "X_APPLICATION_ID header %s contains not allowed chars".formatted(appIdHeader));
    }

    return appIdHeader;
  }
}
