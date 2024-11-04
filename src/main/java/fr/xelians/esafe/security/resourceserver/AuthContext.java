/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.resourceserver.JwtClaimExtractor.*;

import fr.xelians.esafe.common.exception.technical.InternalException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/*
 * @author Youcef Bouhaddouza
 */
public final class AuthContext {

  private AuthContext() {}

  public static Long getTenant() {
    Long tenant = getAuthenticationDetails().getTenantId();
    if (tenant == null) {
      throw new InternalException(
          "Failed to get tenant from Authentication Context", "Tenant is not defined");
    }
    return tenant;
  }

  public static String getApplicationId() {
    return getAuthenticationDetails().getApplicationId();
  }

  private static TokenAuthenticationDetails getAuthenticationDetails() {
    return (TokenAuthenticationDetails)
        SecurityContextHolder.getContext().getAuthentication().getDetails();
  }

  public static String getUserIdentifier() {
    return extractUserIdentifier(getPrincipal());
  }

  public static String getOrganizationIdentifier() {
    return extractOrganizationId(getPrincipal());
  }

  private static Jwt getPrincipal() {
    return (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
