/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.domain;

import fr.xelians.esafe.common.exception.technical.InternalException;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {

  private AuthContext() {}

  public static Long getTenant() {
    Long tenant =
        ((JwtAuthenticationDetails)
                SecurityContextHolder.getContext().getAuthentication().getDetails())
            .getTenant();
    if (tenant == null) {
      throw new InternalException(
          "Failed to get tenant from Authentification Context", "Tenant is not defined");
    }
    return tenant;
  }

  public static String getApplicationId() {
    return ((JwtAuthenticationDetails)
            SecurityContextHolder.getContext().getAuthentication().getDetails())
        .getApplicationId();
  }

  public static Long getUserId() {
    AuthUserDetails authUserDetails =
        (AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authUserDetails.getId();
  }

  public static String getUsername() {
    AuthUserDetails authUserDetails =
        (AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authUserDetails.getUsername();
  }

  public static String getUserIdentifier() {
    AuthUserDetails authUserDetails =
        (AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authUserDetails.getIdentifier();
  }

  public static Long getOrganizationId() {
    AuthUserDetails authUserDetails =
        (AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return authUserDetails.getOrganizationId();
  }
}
