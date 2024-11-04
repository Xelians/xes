/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/*
 * @author Youcef Bouhaddouza
 */
@Getter
public class AccessKeyAuthentication extends AbstractAuthenticationToken {

  private final Object principal;

  private String accessKey;

  private AccessKeyAuthentication(
      Object principal, Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.principal = principal;
    super.setAuthenticated(true);
    eraseCredentials();
  }

  private AccessKeyAuthentication(String accessKey) {
    super(Collections.emptyList());
    this.principal = accessKey;
    this.accessKey = accessKey;
    super.setAuthenticated(false);
  }

  @Override
  public Object getCredentials() {
    return accessKey;
  }

  public static AccessKeyAuthentication unauthenticated(String accessKey) {
    return new AccessKeyAuthentication(accessKey);
  }

  public static AccessKeyAuthentication authenticated(
      Object principal, Collection<? extends GrantedAuthority> authorities) {
    return new AccessKeyAuthentication(principal, authorities);
  }

  public void eraseCredentials() {
    super.eraseCredentials();
    this.accessKey = null;
  }
}
