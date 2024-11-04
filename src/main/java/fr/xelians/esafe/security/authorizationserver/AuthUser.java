/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.organization.domain.Role.GlobalRole;
import fr.xelians.esafe.organization.domain.TenantRole;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.security.grantedauthority.TenantGrantedAuthority;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/*
 * @author Youcef Bouhaddouza
 */
@Getter
class AuthUser implements UserDetails {

  private final Long id;
  private final String username;
  @JsonIgnore private final String password;
  @JsonIgnore private final String accessKey;
  private final String email;
  private final String identifier;
  private final String organizationId;
  private final boolean enabled;

  private final Set<String> accessContracts;
  private final Set<String> ingestContracts;

  private final List<GrantedAuthority> authorities = new ArrayList<>();

  public AuthUser(UserDb userDb) {
    this.id = userDb.getId();
    this.username = userDb.getUsername();
    this.email = userDb.getEmail();
    this.accessKey = userDb.getAccessKey();
    this.password = userDb.getPassword();
    this.identifier = userDb.getIdentifier();
    this.organizationId = userDb.getOrganization().getIdentifier();
    this.accessContracts = userDb.getAccessContracts();
    this.ingestContracts = userDb.getIngestContracts();
    this.enabled = userDb.getStatus() == Status.ACTIVE;
    mapAuthorities(userDb);
  }

  private void mapAuthorities(UserDb userDb) {
    this.authorities.addAll(mapToSimpleGrantedAuthorities(userDb.getGlobalRoles()));
    this.authorities.addAll(mapToTenantGrantedAuthorities(userDb.getTenantRoles()));
  }

  private static @NotNull List<SimpleGrantedAuthority> mapToSimpleGrantedAuthorities(
      Set<GlobalRole> globalRoles) {
    return globalRoles.stream().map(Enum::toString).map(SimpleGrantedAuthority::new).toList();
  }

  private static @NotNull List<GrantedAuthority> mapToTenantGrantedAuthorities(
      Set<String> tenantRoles) {
    return tenantRoles.stream()
        .map(TenantRole::new)
        .map(
            tenantRole ->
                (GrantedAuthority)
                    new TenantGrantedAuthority(
                        tenantRole.getTenant(), tenantRole.getRoleName().name()))
        .toList();
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return new ArrayList<>(authorities);
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthUser user = (AuthUser) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public String toString() {
    return "AuthUser{"
        + "id="
        + id
        + ", username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + ", email='"
        + email
        + '\''
        + ", identifier='"
        + identifier
        + '\''
        + ", organizationId="
        + organizationId
        + '}';
  }
}
