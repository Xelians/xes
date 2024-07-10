/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import static fr.xelians.esafe.common.constant.Api.LOGOUT;
import static fr.xelians.esafe.common.constant.Api.REFRESH;
import static fr.xelians.esafe.common.constant.Api.SIGNIN;
import static fr.xelians.esafe.common.constant.Api.SIGNUP;
import static fr.xelians.esafe.common.constant.Api.V1;

import fr.xelians.esafe.authentication.domain.ApiKeyAuthenticationFilter;
import fr.xelians.esafe.authentication.domain.JwtAuthenticationFilter;
import fr.xelians.esafe.authentication.service.AuthUserDetailsService;
import fr.xelians.esafe.authentication.service.AuthenticationService;
import fr.xelians.esafe.common.servlet.BigRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Allows Spring to find and automatically apply the class to the global Web Security
// provides AOP security on methods. It enables @PreAuthorize, @PostAuthorize, it also supports
// JSR-250
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true
    // jsr250Enabled = true,
    )
public class SecurityConfig {

  @Bean
  public BigRequestFilter bigRequestFilter() {
    return new BigRequestFilter();
  }

  @Bean
  public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
      AuthenticationService authenticationService, AuthUserDetailsService authUserDetailsService) {
    return new ApiKeyAuthenticationFilter(authUserDetailsService);
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      AuthenticationService authenticationService, AuthUserDetailsService authUserDetailsService) {
    return new JwtAuthenticationFilter(authenticationService, authUserDetailsService);
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity httpSecurity,
      JwtAuthenticationConfig unauthorizedHandler,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      ApiKeyAuthenticationFilter apiKeyAuthenticationFilter,
      BigRequestFilter bigRequestFilter)
      throws Exception {

    httpSecurity
        .sessionManagement(e -> e.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(e -> e.configure(httpSecurity))
        .csrf(AbstractHttpConfigurer::disable)
        .exceptionHandling(e -> e.authenticationEntryPoint(unauthorizedHandler))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/swagger-ui*/**",
                        "/actuator/**",
                        "/readyz",
                        "/livez",
                        "/auth/**",
                        "/error/**")
                    .permitAll()
                    .requestMatchers(V1 + SIGNIN + "/**", V1 + SIGNUP + "/**", V1 + REFRESH + "/**")
                    .permitAll()
                    .requestMatchers(V1 + LOGOUT)
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(bigRequestFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return httpSecurity.build();
  }
}
