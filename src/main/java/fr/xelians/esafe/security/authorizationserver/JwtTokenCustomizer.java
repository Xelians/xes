/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import static fr.xelians.esafe.security.JwtClaimNames.*;
import static java.util.stream.Collectors.*;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

import fr.xelians.esafe.security.grantedauthority.TenantGrantedAuthority;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * Component within OAuth 2.0 authentication that allows adding or modifying claims in an access
 * token or refresh token. It is often used to include additional information, such as the resource
 * owner’s authorities (roles, permissions, groups), custom identifiers, or any other details that
 * may be needed by a specific resource server.
 *
 * @author Youcef Bouhaddouza
 */
class JwtTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  @Override
  public void customize(JwtEncodingContext context) {
    if (CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
      return;
    }
    Authentication authentication = context.getPrincipal();
    AuthUser authUser = (AuthUser) authentication.getPrincipal();
    if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
      context.getClaims().claim(ORGANIZATION_ID, authUser.getOrganizationId());
      context.getClaims().claim(USER_ID, authUser.getIdentifier());
      formatRolesClaims(context, authUser.getAuthorities());
    }
  }

  private static void formatRolesClaims(
      JwtEncodingContext context, Collection<? extends GrantedAuthority> authorities) {
    context
        .getClaims()
        .claim(
            ROLES,
            Map.of(
                GLOBAL_ROLES,
                formatGlobalRoles(authorities),
                TENANT_ROLES,
                formatTenantRoles(authorities)));
  }

  private static @NotNull Set<String> formatGlobalRoles(
      Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .filter(SimpleGrantedAuthority.class::isInstance)
        .map(GrantedAuthority::getAuthority)
        .collect(toSet());
  }

  private static @NotNull Map<Long, Set<String>> formatTenantRoles(
      Collection<? extends GrantedAuthority> authorities) {
    return authorities.stream()
        .filter(TenantGrantedAuthority.class::isInstance)
        .collect(
            groupingBy(
                grantedAuthority -> ((TenantGrantedAuthority) grantedAuthority).getTenant(),
                mapping(GrantedAuthority::getAuthority, toSet())));
  }
}
