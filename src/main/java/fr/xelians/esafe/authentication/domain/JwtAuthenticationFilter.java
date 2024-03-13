/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.domain;

import static fr.xelians.esafe.common.constant.Header.AUTHORIZATION;

import fr.xelians.esafe.authentication.service.AuthUserDetailsService;
import fr.xelians.esafe.authentication.service.AuthenticationService;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.utils.HeaderUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER = "Bearer ";
  private static final int BEARER_LEN = BEARER.length();
  public static final String BAD_ACCESS_CONTRACT =
      "User '{}' with tenant '{}' has bad access contract '{}'";

  private final AuthenticationService authenticationService;
  private final AuthUserDetailsService authUserDetailsService;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    // Do not authenticate an already authenticated user
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      String authHeader = request.getHeader(AUTHORIZATION);

      // VITAM patch for testing only - use Spring profile instead
      if (authHeader == null) {
        String app = request.getHeader(Header.X_APPLICATION_ID);
        authHeader = StringUtils.substringAfter(app, "|!|");
      }

      if (authHeader != null && authHeader.length() > BEARER_LEN && authHeader.startsWith(BEARER)) {
        String accessToken = authHeader.substring(BEARER_LEN);
        try {
          // Get the username from the jwt token.
          // Throw an exception if the token in not valid, expired, etc.
          String username = authenticationService.getUsernameFromAccessToken(accessToken);
          if (username != null) {
            doJwtAuthentification(request, username);
          }
        } catch (Exception e) {
          // Catch all because we can potentially get a lot of runtime exceptions :
          // AuthenticationException, BadRequestException, *JwtException, etc.
          log.warn(e.getMessage());
        }
      }
    }
    // Execute the following filter.
    filterChain.doFilter(request, response);

    // If the request needs an authentication and all
    // authentication fail then we return a 401  - UNAUTHORIZED
  }

  // At this stage, the access token is validated. Then, we check the tenant and
  // access contract (if any). We authenticate the user, and we set its authorities
  // accordingly.
  private void doJwtAuthentification(HttpServletRequest request, String username) {

    // TODO Get user details from the jwt token (not from database)
    AuthUserDetails userDetails = authUserDetailsService.getAuthUserDetails(username);

    String tenantHeader = request.getHeader(Header.X_TENANT_ID);
    String accessContractHeader = request.getHeader(Header.X_ACCESS_CONTRACT_ID);
    String applicationHeader = request.getHeader(Header.X_APPLICATION_ID);

    // VITAM patch for testing only - use Spring profile instead
    applicationHeader = StringUtils.substringBefore(applicationHeader, "|!|");

    if (StringUtils.isBlank(accessContractHeader)) {
      if (StringUtils.isBlank(tenantHeader)) {
        authenticateWithGlobalAuthorities(request, userDetails, applicationHeader);
      } else {
        authenticateWithTenantAuthorities(request, userDetails, applicationHeader, tenantHeader);
      }
    } else if (StringUtils.isNotBlank(tenantHeader)) {
      String tenantContract = tenantHeader + ":" + accessContractHeader;
      if (userDetails.getAccessContracts().contains(tenantContract)) {
        authenticateWithTenantAuthorities(request, userDetails, applicationHeader, tenantHeader);
      } else {
        log.warn(
            BAD_ACCESS_CONTRACT, userDetails.getUsername(), tenantHeader, accessContractHeader);
        // We limit to roles that don't need access contract (this is not granular!)
        authenticateWithGlobalAuthorities(request, userDetails, applicationHeader);
      }
    } else {
      authenticateWithGlobalAuthorities(request, userDetails, applicationHeader);
    }
  }

  private void authenticateWithTenantAuthorities(
      HttpServletRequest request,
      AuthUserDetails userDetails,
      String applicationHeader,
      String tenantHeader) {

    String applicationId = HeaderUtils.getApplicationId(applicationHeader);
    Long tenant = HeaderUtils.getTenant(tenantHeader);

    var authorities = userDetails.getTenantAuthorities(tenant);
    doAuthentication(request, userDetails, applicationId, tenant, authorities);
  }

  private void authenticateWithGlobalAuthorities(
      HttpServletRequest request, AuthUserDetails userDetails, String applicationHeader) {

    String applicationId = HeaderUtils.getApplicationId(applicationHeader);
    var authorities = userDetails.getGlobalAuthorities();
    doAuthentication(request, userDetails, applicationId, null, authorities);
  }

  // Create and save the authentication object of the session in the SecurityContextHolder
  private void doAuthentication(
      HttpServletRequest request,
      AuthUserDetails userDetails,
      String applicationId,
      Long tenant,
      Collection<? extends GrantedAuthority> authorities) {

    // Set the authorities used during the authentication process
    var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    authentication.setDetails(new JwtAuthenticationDetails(request, tenant, applicationId));
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
