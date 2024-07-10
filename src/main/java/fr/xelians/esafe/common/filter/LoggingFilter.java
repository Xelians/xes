/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@AllArgsConstructor
public class LoggingFilter extends CommonsRequestLoggingFilter {

  private final String[] pathsToIgnore;

  @Override
  protected boolean shouldLog(HttpServletRequest request) {
    return !StringUtils.startsWithAny(request.getRequestURI(), pathsToIgnore);
  }
}
