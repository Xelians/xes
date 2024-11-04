/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.stub;

import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.ROLE_ROOT_ADMIN;

import fr.xelians.esafe.organization.domain.Root;
import fr.xelians.esafe.security.resourceserver.TokenAuthenticationDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.NonNull;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/*
 * @author Julien Cornille
 */
public class StubBearerTokenAuthenticationFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

    Map<String, Object> attributes =
        Map.of(
            "sub", "admin",
            "user_id", Root.USER_IDENTIFIER,
            "organization_id", Root.ORGA_IDENTIFIER);

    JwtAuthenticationToken jwtAuthentication =
        new JwtAuthenticationToken(
            new Jwt(
                "azerty",
                Instant.MIN,
                Instant.MAX,
                Collections.singletonMap("key", "value"),
                attributes),
            AuthorityUtils.createAuthorityList(ROLE_ROOT_ADMIN));
    jwtAuthentication.setAuthenticated(true);
    jwtAuthentication.setDetails(new TokenAuthenticationDetails((request)));

    SecurityContextHolder.getContext().setAuthentication(jwtAuthentication);
    filterChain.doFilter(request, response);
  }
}
