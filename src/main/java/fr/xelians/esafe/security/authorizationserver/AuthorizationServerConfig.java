/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyAuthenticationConverter;
import fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyAuthenticationProvider;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * OAuth 2.0 Authorization Server configuration. The Authorization Server is responsible for issuing
 * OAuth 2.0 tokens (such as access tokens and refresh tokens) to clients after authenticating
 * users, enabling secure access to protected resources on a Resource Server.
 *
 * @author Youcef Bouhaddouza
 */
@Configuration(proxyBeanMethods = false)
class AuthorizationServerConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  SecurityFilterChain authorizationServerSecurityFilterChain(
      HttpSecurity http,
      OAuth2AccessKeyAuthenticationProvider accessKeyGrantTypeAuthenticationProvider)
      throws Exception {
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
        .oidc(Customizer.withDefaults())
        .tokenEndpoint(
            tokenEndpoint ->
                tokenEndpoint
                    .accessTokenRequestConverter(new OAuth2AccessKeyAuthenticationConverter())
                    .authenticationProvider(accessKeyGrantTypeAuthenticationProvider));

    http.exceptionHandling(
        exception ->
            exception.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"),
                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

    return http.build();
  }

  /**
   * Component used to store, retrieve, and manage OAuth2 authorization data. This data typically
   * includes information about the authorization grants, the associated client, the user, and the
   * scopes of access granted.
   *
   * @return OAuth2AuthorizationService
   */
  @Bean
  OAuth2AuthorizationService oAuth2AuthorizationService() {
    return new InMemoryOAuth2AuthorizationService();
  }

  @Bean
  OAuth2AccessKeyAuthenticationProvider accessKeyGrantTypeAuthenticationProvider(
      OAuth2AuthorizationService authorizationService,
      OAuth2TokenGenerator<?> tokenGenerator,
      @Qualifier("accessKeyAuthenticationProvider")
          AuthenticationProvider userAuthenticationProvider) {
    return new OAuth2AccessKeyAuthenticationProvider(
        authorizationService, tokenGenerator, userAuthenticationProvider);
  }

  /**
   * Component provides a standardized way to generate various types of tokens required in OAuth2
   * flows, such as access tokens and refresh tokens. It ensures that the tokens adhere to security
   * standards and contain the necessary information for resource access.
   *
   * @param jwkSource JwkSource
   * @param tokenCustomizer TokenCustomizer
   * @return OAuth2TokenGenerator
   */
  @Bean
  OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(
      JWKSource<SecurityContext> jwkSource,
      OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer) {
    NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setJwtCustomizer(tokenCustomizer);
    OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
    OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
    return new DelegatingOAuth2TokenGenerator(
        jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
  }

  /**
   * Component provides methods to retrieve one or more JWKs. This is essential for validating the
   * JWT signatures based on the public keys associated with the signing keys used to create the
   * JWTs.
   *
   * @param jwkProperties JwkProperties
   * @return JWKSource
   */
  @Bean
  JWKSource<SecurityContext> jwkSource(JwkProperties jwkProperties) {
    KeyPair keyPair = jwkProperties.getKeyPair();
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(jwkProperties.getKeyId())
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  @Bean
  OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
    return new JwtTokenCustomizer();
  }
}
