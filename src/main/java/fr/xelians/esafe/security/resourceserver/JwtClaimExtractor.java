/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.JwtClaimNames.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.jwt.Jwt;

/*
 * Class for Extracting JWT Claims
 *
 * @author Youcef Bouhaddouza
 */
@UtilityClass
final class JwtClaimExtractor {

  public static String extractOrganizationId(Jwt jwt) {
    return (String) getClaimsByKey(jwt, ORGANIZATION_ID);
  }

  public static String extractUserIdentifier(Jwt jwt) {
    return (String) getClaimsByKey(jwt, USER_ID);
  }

  @SuppressWarnings("unchecked")
  public static List<String> extractGlobalRoles(Jwt jwt) {
    return ((Map<String, List<String>>) extractRoles(jwt)).getOrDefault(GLOBAL_ROLES, emptyList());
  }

  @SuppressWarnings("unchecked")
  public static Map<String, List<String>> extractTenantRoles(Jwt jwt) {
    return ((Map<String, Map<String, List<String>>>) extractRoles(jwt))
        .getOrDefault(TENANT_ROLES, emptyMap());
  }

  private static Object extractRoles(Jwt jwt) {
    return jwt.getClaims().getOrDefault(ROLES, emptyMap());
  }

  private static Object getClaimsByKey(Jwt jwt, String claimKey) {
    return jwt.getClaims().get(claimKey);
  }
}
