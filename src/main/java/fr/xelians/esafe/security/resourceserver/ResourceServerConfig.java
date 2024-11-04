/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.organization.domain.Role.GlobalRole.Names.*;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.Names.*;

import jakarta.validation.constraints.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server configuration responsible for securing APIs and ensuring that only
 * authorized clients/users can access them using OAuth2 tokens. The resource server is responsible
 * for validating access tokens and protecting endpoints based on those tokens (Roles, Scopes).
 *
 * @author Youcef Bouhaddouza
 */
@Configuration
class ResourceServerConfig {

  @Bean
  @Order(2)
  SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(
        rs ->
            rs.jwt(
                    jwtConfigurer ->
                        jwtConfigurer.jwtAuthenticationConverter(
                            new JwtAuthenticationTokenConverter()))
                .addObjectPostProcessor(getObjectPostProcessor()));

    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(getOpenedResources())
                    .permitAll()
                    .anyRequest()
                    .authenticated());
    http.formLogin(Customizer.withDefaults());
    return http.build();
  }

  private @NotNull ObjectPostProcessor<BearerTokenAuthenticationFilter> getObjectPostProcessor() {
    return new ObjectPostProcessor<>() {
      @Override
      public <O extends BearerTokenAuthenticationFilter> O postProcess(
          O bearerTokenAuthenticationFilter) {
        bearerTokenAuthenticationFilter.setAuthenticationDetailsSource(
            TokenAuthenticationDetails::new);
        return bearerTokenAuthenticationFilter;
      }
    };
  }

  /**
   * Opened endpoints without authentication
   *
   * @return String[]
   */
  private String[] getOpenedResources() {
    return new String[] {
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/actuator/**",
      "/readyz",
      "/livez",
      "/error/**",
      V1 + SIGNUP + "/**"
    };
  }

  /**
   * Allows defining a structure where roles can inherit from other roles. For example, if
   * ROLE_ROOT_ADMIN inherits from ROLE_ADMIN, a user with the ROLE_ROOT_ADMIN will also have the
   * permissions of ROLE_ADMIN.
   *
   * @return RoleHierarchy
   */
  @Bean
  public RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.withRolePrefix("")
        .role(ROLE_ROOT_ADMIN)
        .implies(ROLE_ADMIN, ROLE_DEPRECATED)
        .role(ROLE_ADMIN)
        .implies(ROLE_ARCHIVE_MANAGER)
        .role(ROLE_ARCHIVE_MANAGER)
        .implies(ROLE_ARCHIVE_READER)
        .build();
  }
}
