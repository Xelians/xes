/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.domain;

import static fr.xelians.esafe.common.constant.Header.X_API_KEY_ID;

import fr.xelians.esafe.authentication.service.AuthUserDetailsService;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.HeaderUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private final AuthUserDetailsService authUserDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String apiKey = request.getHeader(X_API_KEY_ID);
    if (StringUtils.isNotBlank(apiKey)) {
      log.debug("Trying to authenticate with apikey={}", apiKey);
      final var possibleUserDetails = authUserDetailsService.loadUserByApiKey(apiKey);
      possibleUserDetails.ifPresentOrElse(
          authUserDetails -> authenticateWithGlobalAuthorities(request, authUserDetails),
          () -> {
            throw new BadCredentialsException("ApiKey not found");
          });
    }

    filterChain.doFilter(request, response);
  }

  private Long extractTenantIdentifier(HttpServletRequest request) {
    long tenant;
    try {
      tenant = HeaderUtils.getTenant(request.getHeader(Header.X_TENANT_ID));

    } catch (BadRequestException exception) {
      String apiKey = request.getHeader(X_API_KEY_ID);
      String[] parts = apiKey.split("-");
      String numberString = parts[parts.length - 1];

      tenant = Long.parseLong(numberString);
    }

    return tenant;
  }

  private void authenticateWithGlobalAuthorities(
      HttpServletRequest request, AuthUserDetails userDetails) {
    String applicationHeader = request.getHeader(Header.X_APPLICATION_ID);

    String applicationId = HeaderUtils.getApplicationId(applicationHeader);
    var authorities = userDetails.getGlobalAuthorities();
    doAuthentication(request, userDetails, applicationId, authorities);
  }

  private void doAuthentication(
      HttpServletRequest request,
      AuthUserDetails userDetails,
      String applicationId,
      Collection<? extends GrantedAuthority> authorities) {

    // Set the authorities used during the authentication process
    var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    authentication.setDetails(
        new JwtAuthenticationDetails(request, extractTenantIdentifier(request), applicationId));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
