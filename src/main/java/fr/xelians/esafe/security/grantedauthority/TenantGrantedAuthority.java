/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.grantedauthority;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;

/*
 * @author Youcef Bouhaddouza
 */
@EqualsAndHashCode
@ToString
public class TenantGrantedAuthority implements GrantedAuthority {

  @Getter private final Long tenant;

  private final String role;

  public TenantGrantedAuthority(Long tenant, String role) {
    Assert.notNull(tenant, "Tenant is required");
    Assert.hasText(role, "A granted authority textual representation is required");
    this.role = role;
    this.tenant = tenant;
  }

  @Override
  public String getAuthority() {
    return role;
  }
}
