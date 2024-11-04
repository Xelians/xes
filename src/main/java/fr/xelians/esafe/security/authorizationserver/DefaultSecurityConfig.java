/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import fr.xelians.esafe.organization.accesskey.AccessKeyProperties;
import fr.xelians.esafe.organization.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.util.Assert;

/*
 * Configuration Class for Core Spring Security Components Without Utilizing OAuth2.
 *
 * @author Youcef Bouhaddouza
 */
@Configuration(proxyBeanMethods = false)
@Slf4j
class DefaultSecurityConfig {

  /**
   * Component responsible for retrieving user-related data. It is primarily used for authentication
   * and authorization processes, providing the UserDetails object that contains the necessary
   * information about the user, such as username, password, roles, and account status.
   *
   * @param userRepository UserRepository
   * @return UserDetailsService
   */
  @Bean
  UserDetailsService userDetailsService(UserRepository userRepository) {
    return username -> {
      Assert.notNull(username, "username cannot be null");
      return userRepository
          .findByUsername(username)
          .map(AuthUser::new)
          .orElseThrow(() -> new UsernameNotFoundException(username + " not found"));
    };
  }

  /**
   * Handles user authentication using username and password by loading user details from the
   * database. It is used during the OAuth2 Authorization Code Flow for user authentication
   *
   * @param userDetailsService UserDetailsService
   * @param passwordEncoder PasswordEncoder
   * @return AuthenticationProvider
   */
  @Bean
  AuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  /**
   * Handles user authentication using access key. It is used during the OAuth2 Authorization Access
   * Key Flow for user authentication {@link
   * fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyAuthenticationProvider}
   *
   * @param userRepository UserRepository
   * @param passwordEncoder PasswordEncoder
   * @param accessKeyProperties AccessKeyProperties
   * @return AuthenticationProvider
   */
  // TODO : add it in a filter et use authenticationManger to authenticate user with
  @Bean
  AuthenticationProvider accessKeyAuthenticationProvider(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      AccessKeyProperties accessKeyProperties) {
    JwtDecoder jwtDecoder =
        NimbusJwtDecoder.withPublicKey(accessKeyProperties.getPublicKey()).build();
    AccessKeyAuthenticationProvider accessKeyAuthenticationProvider =
        new AccessKeyAuthenticationProvider(userRepository, passwordEncoder, jwtDecoder);

    if (!accessKeyProperties.isPersisted()) {
      log.warn(
          "You have disabled the revocation check for the access key. This is not recommended for production environments.");
      accessKeyAuthenticationProvider.setAccessKeyRevoked((ake, aka) -> false);
    }

    return accessKeyAuthenticationProvider;
  }

  /**
   * Handles password encoding and verification. It provides a standard way to encode passwords and
   * check if a provided password matches the stored encoded password.
   *
   * @return PasswordEncoder
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
