/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.domain;

import jakarta.servlet.http.HttpServletRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

  private final Long tenant;
  private final String applicationId;

  public JwtAuthenticationDetails(HttpServletRequest context, Long tenant, String applicationId) {
    super(context);
    this.tenant = tenant;
    this.applicationId = applicationId;
  }
}
