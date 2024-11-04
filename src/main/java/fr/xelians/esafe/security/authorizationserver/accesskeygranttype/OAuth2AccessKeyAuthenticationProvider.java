/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.*;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Custom Component that handles the authentication process for OAuth 2.0 authorization access key
 * grant. It is responsible for processing the authorization access key, which involves exchanging
 * an access key for an access token, refresh token and authenticating the user.
 *
 * @author Youcef Bouhaddouza
 */
public class OAuth2AccessKeyAuthenticationProvider implements AuthenticationProvider {

  private static final String ERROR_URI =
      "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";
  private static final String INVALID_ACCESS_KEY = "invalid_access_key";
  private static final OAuth2TokenType ID_TOKEN_TOKEN_TYPE =
      new OAuth2TokenType(OidcParameterNames.ID_TOKEN);
  private final OAuth2AuthorizationService authorizationService;
  private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
  private final AuthenticationProvider userAuthenticationProvider;
  private SessionRegistry sessionRegistry;

  public OAuth2AccessKeyAuthenticationProvider(
      OAuth2AuthorizationService authorizationService,
      OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator,
      AuthenticationProvider userAuthenticationProvider) {
    Assert.notNull(authorizationService, "authorizationService cannot be null");
    Assert.notNull(tokenGenerator, "tokenGenerator cannot be null");
    Assert.notNull(userAuthenticationProvider, "userAuthenticationProvider cannot be null");
    this.authorizationService = authorizationService;
    this.tokenGenerator = tokenGenerator;
    this.userAuthenticationProvider = userAuthenticationProvider;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    OAuth2AccessKeyRequestAuthenticationToken accessKeyGrantAuthentication =
        (OAuth2AccessKeyRequestAuthenticationToken) authentication;

    OAuth2ClientAuthenticationToken clientPrincipal =
        getAuthenticatedClientElseThrowInvalidClient(accessKeyGrantAuthentication);

    RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
    if (registeredClient == null
        || !registeredClient
            .getAuthorizationGrantTypes()
            .contains(accessKeyGrantAuthentication.getGrantType())) {
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
    }

    if (!registeredClient.getScopes().containsAll(accessKeyGrantAuthentication.getScopes())) {
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
    }

    Authentication accessKeyAuthentication =
        AccessKeyAuthentication.unauthenticated(accessKeyGrantAuthentication.getAccessKey());
    try {
      accessKeyAuthentication = userAuthenticationProvider.authenticate(accessKeyAuthentication);
    } catch (AuthenticationException authenticationException) {
      OAuth2Error error =
          new OAuth2Error(INVALID_ACCESS_KEY, "OAuth 2.0 Parameter : access_key", ERROR_URI);
      throw new OAuth2AuthenticationException(error, error.toString(), authenticationException);
    }

    DefaultOAuth2TokenContext.Builder tokenContextBuilder =
        DefaultOAuth2TokenContext.builder()
            .registeredClient(registeredClient)
            .principal(accessKeyAuthentication)
            .authorizationServerContext(AuthorizationServerContextHolder.getContext())
            .authorizedScopes(accessKeyGrantAuthentication.getScopes())
            .authorizationGrantType(accessKeyGrantAuthentication.getGrantType())
            .authorizationGrant(accessKeyGrantAuthentication);

    // ----- Access Token -----
    OAuth2TokenContext tokenContext =
        tokenContextBuilder.tokenType(OAuth2TokenType.ACCESS_TOKEN).build();
    OAuth2Token generatedAccessToken = tokenGenerator.generate(tokenContext);
    if (generatedAccessToken == null) {
      OAuth2Error error =
          new OAuth2Error(
              OAuth2ErrorCodes.SERVER_ERROR,
              "The token generator failed to generate the access token.",
              ERROR_URI);
      throw new OAuth2AuthenticationException(error);
    }

    OAuth2Authorization.Builder authorizationBuilder =
        OAuth2Authorization.withRegisteredClient(registeredClient)
            .principalName(accessKeyAuthentication.getName())
            .attribute(Principal.class.getName(), accessKeyAuthentication)
            .authorizationGrantType(accessKeyGrantAuthentication.getGrantType())
            .authorizedScopes(accessKeyGrantAuthentication.getScopes());

    OAuth2AccessToken accessToken =
        createAccessToken(authorizationBuilder, generatedAccessToken, tokenContext);

    // ----- Refresh Token -----
    OAuth2RefreshToken refreshToken = null;
    // Do not issue refresh token to public client
    if (registeredClient
        .getAuthorizationGrantTypes()
        .contains(AuthorizationGrantType.REFRESH_TOKEN)) {
      tokenContext = tokenContextBuilder.tokenType(OAuth2TokenType.REFRESH_TOKEN).build();
      OAuth2Token generatedRefreshToken = this.tokenGenerator.generate(tokenContext);
      if (generatedRefreshToken != null) {
        if (!(generatedRefreshToken instanceof OAuth2RefreshToken)) {
          OAuth2Error error =
              new OAuth2Error(
                  OAuth2ErrorCodes.SERVER_ERROR,
                  "The token generator failed to generate a valid refresh token.",
                  ERROR_URI);
          throw new OAuth2AuthenticationException(error);
        }

        refreshToken = (OAuth2RefreshToken) generatedRefreshToken;
        authorizationBuilder.refreshToken(refreshToken);
      }
    }

    // ----- ID token -----
    OidcIdToken idToken;
    if (accessKeyGrantAuthentication.getScopes().contains(OidcScopes.OPENID)) {
      SessionInformation sessionInformation = getSessionInformation(accessKeyAuthentication);
      if (sessionInformation != null) {
        try {
          // Compute (and use) hash for Session ID
          sessionInformation =
              new SessionInformation(
                  sessionInformation.getPrincipal(),
                  createHash(sessionInformation.getSessionId()),
                  sessionInformation.getLastRequest());
        } catch (NoSuchAlgorithmException ex) {
          OAuth2Error error =
              new OAuth2Error(
                  OAuth2ErrorCodes.SERVER_ERROR,
                  "Failed to compute hash for Session ID.",
                  ERROR_URI);
          throw new OAuth2AuthenticationException(error);
        }
        tokenContextBuilder.put(SessionInformation.class, sessionInformation);
      }
      // @formatter:off
      tokenContext =
          tokenContextBuilder
              .tokenType(ID_TOKEN_TOKEN_TYPE)
              .authorization(
                  authorizationBuilder
                      .build()) // ID token customizer may need access to the access token and/or
              // refresh token
              .build();
      // @formatter:on
      OAuth2Token generatedIdToken = this.tokenGenerator.generate(tokenContext);
      if (!(generatedIdToken instanceof Jwt)) {
        OAuth2Error error =
            new OAuth2Error(
                OAuth2ErrorCodes.SERVER_ERROR,
                "The token generator failed to generate the ID token.",
                ERROR_URI);
        throw new OAuth2AuthenticationException(error);
      }

      idToken =
          new OidcIdToken(
              generatedIdToken.getTokenValue(),
              generatedIdToken.getIssuedAt(),
              generatedIdToken.getExpiresAt(),
              ((Jwt) generatedIdToken).getClaims());
      authorizationBuilder.token(
          idToken,
          (metadata) ->
              metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, idToken.getClaims()));
    } else {
      idToken = null;
    }

    OAuth2Authorization authorization = authorizationBuilder.build();
    this.authorizationService.save(authorization);

    Map<String, Object> additionalParameters = Collections.emptyMap();
    if (idToken != null) {
      additionalParameters = new HashMap<>();
      additionalParameters.put(OidcParameterNames.ID_TOKEN, idToken.getTokenValue());
    }

    return new OAuth2AccessTokenAuthenticationToken(
        registeredClient, accessKeyAuthentication, accessToken, refreshToken, additionalParameters);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return OAuth2AccessKeyRequestAuthenticationToken.class.isAssignableFrom(authentication);
  }

  /**
   * Sets the {@link SessionRegistry} used to track OpenID Connect sessions.
   *
   * @param sessionRegistry the {@link SessionRegistry} used to track OpenID Connect sessions
   */
  public void setSessionRegistry(SessionRegistry sessionRegistry) {
    Assert.notNull(sessionRegistry, "sessionRegistry cannot be null");
    this.sessionRegistry = sessionRegistry;
  }

  private SessionInformation getSessionInformation(Authentication principal) {
    SessionInformation sessionInformation = null;
    if (this.sessionRegistry != null) {
      List<SessionInformation> sessions =
          this.sessionRegistry.getAllSessions(principal.getPrincipal(), false);
      if (!CollectionUtils.isEmpty(sessions)) {
        sessionInformation = sessions.getFirst();
        if (sessions.size() > 1) {
          // Get the most recent session
          sessions = new ArrayList<>(sessions);
          sessions.sort(Comparator.comparing(SessionInformation::getLastRequest));
          sessionInformation = sessions.getLast();
        }
      }
    }
    return sessionInformation;
  }

  private static String createHash(String value) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
  }

  private static OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
      Authentication authentication) {
    OAuth2ClientAuthenticationToken clientPrincipal = null;
    if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(
        authentication.getPrincipal().getClass())) {
      clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
    }
    if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
      return clientPrincipal;
    }
    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
  }

  private static <T extends OAuth2Token> OAuth2AccessToken createAccessToken(
      OAuth2Authorization.Builder builder, T token, OAuth2TokenContext accessTokenContext) {

    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            token.getTokenValue(),
            token.getIssuedAt(),
            token.getExpiresAt(),
            accessTokenContext.getAuthorizedScopes());
    OAuth2TokenFormat accessTokenFormat =
        accessTokenContext.getRegisteredClient().getTokenSettings().getAccessTokenFormat();
    builder.token(
        accessToken,
        (metadata) -> {
          if (token instanceof ClaimAccessor claimAccessor) {
            metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claimAccessor.getClaims());
          }
          metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
          metadata.put(OAuth2TokenFormat.class.getName(), accessTokenFormat.getValue());
        });

    return accessToken;
  }
}
