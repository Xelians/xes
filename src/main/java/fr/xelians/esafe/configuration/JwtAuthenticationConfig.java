/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.servlet.PbDetail;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtAuthenticationConfig implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) {

    try {
      String url = URLEncoder.encode(request.getServletPath(), StandardCharsets.UTF_8);

      PbDetail pbDetail =
          PbDetail.builder()
              .status(UNAUTHORIZED)
              .title("Authentication failed")
              .message(ex.getMessage())
              .code(ExceptionsUtils.createCode())
              .timestamp(Instant.now())
              .tenant(request.getHeader(Header.X_TENANT_ID))
              .app(request.getHeader(Header.X_APPLICATION_ID))
              .instance(URI.create(url))
              .build();

      String uri = request.getMethod() + " - " + request.getServletPath();
      log.warn(ExceptionsUtils.format(pbDetail, ex.getMessage(), uri));

      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      ExceptionsUtils.MAPPER.writeValue(response.getOutputStream(), pbDetail);
      response.getOutputStream().flush();
    } catch (IOException e) {
      throw new InternalError("Failed to process authentification log", e);
    }
  }
}
