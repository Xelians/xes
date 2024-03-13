/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.organization.domain.role.TenantRole;
import fr.xelians.esafe.organization.entity.UserDb;
import java.util.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class AuthUserDetails implements UserDetails {

  private final Long id;
  private final String username;
  @JsonIgnore private final String password;
  @JsonIgnore private final Set<String> apiKey;
  private final String email;
  private final String identifier;
  private final Long organizationId;

  private final Set<String> accessContracts;
  private final Set<String> ingestContracts;

  private final List<GrantedAuthority> globalAuthorities = new ArrayList<>();
  private final Map<Long, List<GrantedAuthority>> tenantAuthorityMap = new HashMap<>();

  public AuthUserDetails(UserDb userDb) {
    this.id = userDb.getId();
    this.username = userDb.getUsername();
    this.email = userDb.getEmail();
    this.apiKey = userDb.getApiKey();
    this.password = userDb.getPassword();
    this.identifier = userDb.getIdentifier();
    this.organizationId = userDb.getOrganization().getId();
    this.accessContracts = userDb.getAccessContracts();
    this.ingestContracts = userDb.getIngestContracts();

    initRoles(userDb);
  }

  private void initRoles(UserDb userDb) {
    userDb.getGlobalRoles().stream()
        .map(Enum::toString)
        .map(SimpleGrantedAuthority::new)
        .forEach(globalAuthorities::add);

    userDb.getTenantRoles().stream()
        .map(TenantRole::new)
        .forEach(
            tenantRole -> {
              Long tenant = tenantRole.getTenant();
              List<GrantedAuthority> tenantAuthorities =
                  tenantAuthorityMap.computeIfAbsent(tenant, t -> new ArrayList<>());
              var sga = new SimpleGrantedAuthority(tenantRole.getRoleName().name());
              tenantAuthorities.add(sga);
            });
  }

  public Collection<? extends GrantedAuthority> getTenantAuthorities(Long tenant) {
    List<GrantedAuthority> tenantAuthorities = new ArrayList<>(globalAuthorities);
    List<GrantedAuthority> gas = tenantAuthorityMap.get(tenant);
    if (gas != null) {
      tenantAuthorities.addAll(gas);
    }
    return tenantAuthorities;
  }

  public Collection<? extends GrantedAuthority> getGlobalAuthorities() {
    return new ArrayList<>(globalAuthorities);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
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
    return true;
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
    AuthUserDetails user = (AuthUserDetails) o;
    return Objects.equals(id, user.id);
  }

  @Override
  public String toString() {
    return "AuthUserDetails{"
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
