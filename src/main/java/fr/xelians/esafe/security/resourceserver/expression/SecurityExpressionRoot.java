/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.resourceserver.expression;

/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fr.xelians.esafe.security.grantedauthority.TenantGrantedAuthority;
import fr.xelians.esafe.security.resourceserver.TokenAuthenticationDetails;
import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * Base root object for use in Spring Security expression evaluations.
 *
 * @author Luke Taylor
 * @author Evgeniy Cheban
 * @since 3.0
 */
public class SecurityExpressionRoot implements SecurityExpressionOperations {

  private final Supplier<Authentication> authentication;

  private AuthenticationTrustResolver trustResolver;

  private RoleHierarchy roleHierarchy;

  // Custom
  private Collection<? extends GrantedAuthority> authorities;

  private String defaultRolePrefix = "ROLE_";

  /** Allows "permitAll" expression */
  public final boolean permitAll = true;

  /** Allows "denyAll" expression */
  public final boolean denyAll = false;

  private PermissionEvaluator permissionEvaluator;

  public final String read = "read";

  public final String write = "write";

  public final String create = "create";

  public final String delete = "delete";

  public final String admin = "administration";

  /**
   * Creates a new instance
   *
   * @param authentication the {@link Authentication} to use. Cannot be null.
   */
  public SecurityExpressionRoot(Authentication authentication) {
    this(() -> authentication);
  }

  /**
   * Creates a new instance that uses lazy initialization of the {@link Authentication} object.
   *
   * @param authentication the {@link Supplier} of the {@link Authentication} to use. Cannot be
   *     null.
   */
  public SecurityExpressionRoot(Supplier<Authentication> authentication) {
    this.authentication =
        SingletonSupplier.of(
            () -> {
              Authentication value = authentication.get();
              Assert.notNull(value, "Authentication object cannot be null");
              return value;
            });
  }

  @Override
  public final boolean hasAuthority(String authority) {
    return hasAnyAuthority(authority);
  }

  @Override
  public final boolean hasAnyAuthority(String... authorities) {
    return hasAnyAuthorityName(null, authorities);
  }

  @Override
  public final boolean hasRole(String role) {
    return hasAnyRole(role);
  }

  @Override
  public final boolean hasAnyRole(String... roles) {
    return hasAnyAuthorityName(this.defaultRolePrefix, roles);
  }

  /**
   * Then unique customization : replace the default implementation Warning : The validation
   * condition starts from the postulate, no role name is common between the tenant and global roles
   *
   * @param prefix
   * @param roles
   * @return
   */
  private boolean hasAnyAuthorityName(String prefix, String... roles) {
    Collection<? extends GrantedAuthority> authorities = getAuthorities();
    Long tenant = getTenant();
    for (String role : roles) {
      String defaultedRole = getRoleWithDefaultPrefix(prefix, role);
      if (tenant == null) {
        return authorities.contains(new SimpleGrantedAuthority(defaultedRole));
      }

      return authorities.contains(new TenantGrantedAuthority(tenant, defaultedRole))
          || authorities.contains(new SimpleGrantedAuthority(defaultedRole));
    }
    return false;
  }

  private Long getTenant() {
    return ((TokenAuthenticationDetails) getAuthentication().getDetails()).getTenantId();
  }

  /**
   * Custom
   *
   * @return user
   */
  private Collection<? extends GrantedAuthority> getAuthorities() {
    if (this.authorities == null) {
      Collection<? extends GrantedAuthority> userAuthorities = getAuthentication().getAuthorities();
      if (this.roleHierarchy != null) {
        userAuthorities = this.roleHierarchy.getReachableGrantedAuthorities(userAuthorities);
      }
      this.authorities = userAuthorities;
    }
    return this.authorities;
  }

  @Override
  public final Authentication getAuthentication() {
    return this.authentication.get();
  }

  @Override
  public final boolean permitAll() {
    return true;
  }

  @Override
  public final boolean denyAll() {
    return false;
  }

  @Override
  public final boolean isAnonymous() {
    return this.trustResolver.isAnonymous(getAuthentication());
  }

  @Override
  public final boolean isAuthenticated() {
    return this.trustResolver.isAuthenticated(getAuthentication());
  }

  @Override
  public final boolean isRememberMe() {
    return this.trustResolver.isRememberMe(getAuthentication());
  }

  @Override
  public final boolean isFullyAuthenticated() {
    Authentication authentication = getAuthentication();
    return this.trustResolver.isFullyAuthenticated(authentication);
  }

  /**
   * Convenience method to access {@link Authentication#getPrincipal()} from {@link
   * #getAuthentication()}
   *
   * @return
   */
  public Object getPrincipal() {
    return getAuthentication().getPrincipal();
  }

  public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
    this.trustResolver = trustResolver;
  }

  public void setRoleHierarchy(RoleHierarchy roleHierarchy) {
    this.roleHierarchy = roleHierarchy;
  }

  /**
   * Sets the default prefix to be added to {@link #hasAnyRole(String...)} or {@link
   * #hasRole(String)}. For example, if hasRole("ADMIN") or hasRole("ROLE_ADMIN") is passed in, then
   * the role ROLE_ADMIN will be used when the defaultRolePrefix is "ROLE_" (default).
   *
   * <p>If null or empty, then no default role prefix is used.
   *
   * @param defaultRolePrefix the default prefix to add to roles. Default "ROLE_".
   */
  public void setDefaultRolePrefix(String defaultRolePrefix) {
    this.defaultRolePrefix = defaultRolePrefix;
  }

  @Override
  public boolean hasPermission(Object target, Object permission) {
    return this.permissionEvaluator.hasPermission(getAuthentication(), target, permission);
  }

  @Override
  public boolean hasPermission(Object targetId, String targetType, Object permission) {
    return this.permissionEvaluator.hasPermission(
        getAuthentication(), (Serializable) targetId, targetType, permission);
  }

  public void setPermissionEvaluator(PermissionEvaluator permissionEvaluator) {
    this.permissionEvaluator = permissionEvaluator;
  }

  /**
   * Prefixes role with defaultRolePrefix if defaultRolePrefix is non-null and if role does not
   * already start with defaultRolePrefix.
   *
   * @param defaultRolePrefix
   * @param role
   * @return
   */
  private static String getRoleWithDefaultPrefix(String defaultRolePrefix, String role) {
    if (role == null) {
      return role;
    }
    if (defaultRolePrefix == null || defaultRolePrefix.isEmpty()) {
      return role;
    }
    if (role.startsWith(defaultRolePrefix)) {
      return role;
    }
    return defaultRolePrefix + role;
  }
}
