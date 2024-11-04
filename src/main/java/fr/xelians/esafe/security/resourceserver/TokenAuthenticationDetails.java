/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.security.HeaderUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * It encapsulates information specific to the web authentication process, such as the remote
 * address of the user, the session ID, tenant ID and application ID.
 *
 * @author Youcef Bouhaddouza
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class TokenAuthenticationDetails extends WebAuthenticationDetails {

  private final Long tenantId;

  private final String applicationId;

  public TokenAuthenticationDetails(HttpServletRequest context) {
    super(context);
    this.tenantId = extractTenantId(context);
    this.applicationId = extractApplicationId(context);
  }

  private static Long extractTenantId(HttpServletRequest request) {
    String tenantIdHeader = request.getHeader(Header.X_TENANT_ID);
    return HeaderUtils.getTenant(tenantIdHeader);
  }

  private static String extractApplicationId(HttpServletRequest request) {
    String appHeader = request.getHeader(Header.X_APPLICATION_ID);
    return HeaderUtils.getApplicationId(appHeader);
  }
}
