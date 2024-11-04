/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import static fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyParameterNames.ACCESS_KEY_AUTHORIZATION_GRANT_TYPE;
import static java.util.Collections.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.util.Assert;

/*
 * @author Youcef Bouhaddouza
 */
@Getter
public class OAuth2AccessKeyRequestAuthenticationToken
    extends OAuth2AuthorizationGrantAuthenticationToken {

  private final String accessKey;

  private final Set<String> scopes;

  protected OAuth2AccessKeyRequestAuthenticationToken(
      String accessKey,
      Authentication clientPrincipal,
      @Nullable Set<String> scopes,
      Map<String, Object> additionalParameters) {
    super(ACCESS_KEY_AUTHORIZATION_GRANT_TYPE, clientPrincipal, additionalParameters);
    Assert.hasText(accessKey, "accessKey cannot be empty");
    this.accessKey = accessKey;
    this.scopes = unmodifiableSet(scopes != null ? new HashSet<>(scopes) : emptySet());
  }
}
