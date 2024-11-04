/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import static fr.xelians.esafe.organization.accesskey.AccessKeyClaimNames.*;
import static org.apache.commons.lang.StringUtils.isBlank;

import fr.xelians.esafe.organization.repository.UserRepository;
import fr.xelians.esafe.security.authorizationserver.accesskeygranttype.AccessKeyAuthentication;
import java.util.function.BiPredicate;
import lombok.Setter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.util.Assert;

/**
 * Handles user authentication using access key. It is used during the OAuth2 Authorization Access
 * Key Flow for user authentication {@link
 * fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyAuthenticationProvider}
 *
 * @author Youcef Bouhaddouza
 */
public class AccessKeyAuthenticationProvider implements AuthenticationProvider {

  private final UserRepository userRepository;

  private final PasswordEncoder passwordEncoder;

  private final JwtDecoder jwtDecoder;

  private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

  @Setter private BiPredicate<String, String> accessKeyRevoked = this::revoked;

  public AccessKeyAuthenticationProvider(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtDecoder jwtDecoder) {
    this.jwtDecoder = jwtDecoder;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    Assert.isInstanceOf(
        AccessKeyAuthentication.class, authentication, "Only AccessKeyAuthentication is supported");
    AccessKeyAuthentication accessKeyAuthentication = (AccessKeyAuthentication) authentication;

    Jwt jwt;
    try {
      jwt = jwtDecoder.decode(accessKeyAuthentication.getAccessKey());
    } catch (JwtException e) {
      throw new InvalidBearerTokenException("Failed to decode access key", e);
    }

    String userIdentifier = extractUserIdentifier(jwt);
    String organizationIdentifier = extractOrganizationIdentifier(jwt);
    AuthUser authUser =
        retrieveUserByIdentifierAndOrganizationIdentifier(userIdentifier, organizationIdentifier);

    userDetailsChecker.check(authUser);

    if (accessKeyRevoked.test(authUser.getAccessKey(), accessKeyAuthentication.getAccessKey())) {
      throw new BadCredentialsException("Bad credentials");
    }

    return AccessKeyAuthentication.authenticated(authUser, authUser.getAuthorities());
  }

  private boolean revoked(String expectedAccessKey, String actualAccessKey) {
    return isBlank(expectedAccessKey)
        || !this.passwordEncoder.matches(actualAccessKey, expectedAccessKey);
  }

  private AuthUser retrieveUserByIdentifierAndOrganizationIdentifier(
      String userIdentifier, String organizationIdentifier) {
    return userRepository
        .findByIdentifierAndOrganizationIdentifier(userIdentifier, organizationIdentifier)
        .map(AuthUser::new)
        .orElseThrow(
            () ->
                new UsernameNotFoundException(
                    "Failed to find user %s in the organization %s"
                        .formatted(userIdentifier, organizationIdentifier)));
  }

  private static String extractUserIdentifier(Jwt jwt) {
    return (String) jwt.getClaims().get(USER_ID);
  }

  private static String extractOrganizationIdentifier(Jwt jwt) {
    return (String) jwt.getClaims().get(ORGANIZATION_ID);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return AccessKeyAuthentication.class.isAssignableFrom(authentication);
  }
}
