/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.resourceserver.JwtClaimExtractor.extractGlobalRoles;
import static fr.xelians.esafe.security.resourceserver.JwtClaimExtractor.extractTenantRoles;

import fr.xelians.esafe.security.grantedauthority.TenantGrantedAuthority;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Component that converts a JWT into an Authentication object. This is essential in OAuth2 and
 * Spring Security’s resource server setup, as it allows to authenticate users based on the
 * information contained in the JWT.
 *
 * @author Youcef Bouhaddouza
 */
class JwtAuthenticationTokenConverter implements Converter<Jwt, JwtAuthenticationToken> {

  @Override
  public JwtAuthenticationToken convert(Jwt jwt) {
    List<GrantedAuthority> authorities = getGrantedAuthorities(jwt);
    return new JwtAuthenticationToken(jwt, authorities);
  }

  private static @NotNull List<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {
    List<String> globalAuthorities = extractGlobalRoles(jwt);
    Map<String, List<String>> tenantAuthorities = extractTenantRoles(jwt);
    List<GrantedAuthority> authorities =
        new ArrayList<>(convertToGrantedAuthorities(tenantAuthorities));
    authorities.addAll(convertToGrantedAuthorities(globalAuthorities));
    return authorities;
  }

  private static @NotNull List<SimpleGrantedAuthority> convertToGrantedAuthorities(
      List<String> globalAuthorities) {
    return globalAuthorities.stream().map(SimpleGrantedAuthority::new).toList();
  }

  private static @NotNull List<TenantGrantedAuthority> convertToGrantedAuthorities(
      Map<String, List<String>> tenantAuthorities) {
    return tenantAuthorities.entrySet().stream()
        .flatMap(
            entry ->
                entry.getValue().stream()
                    .map(
                        role -> {
                          Long tenant = Long.valueOf(entry.getKey());
                          return new TenantGrantedAuthority(tenant, role);
                        }))
        .toList();
  }
}
