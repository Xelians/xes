package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.resourceserver.JwtTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import fr.xelians.esafe.common.constant.Header;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuthContextTest {

  private static final long X_TENANT_ID_VALUE = 10;

  private static final String X_APPLICATION_ID_VALUE = "application-id-test";

  private static final JwtAuthenticationToken AUTHENTICATION = new JwtAuthenticationToken(JWT);

  @BeforeEach
  void beforeEach() {
    TokenAuthenticationDetails tokenAuthenticationDetails = createTokenAuthenticationDetails();
    AUTHENTICATION.setDetails(tokenAuthenticationDetails);
    SecurityContextHolder.getContext().setAuthentication(AUTHENTICATION);
  }

  @Test
  void getOrganizationId() {
    String organizationIdentifier = AuthContext.getOrganizationIdentifier();

    assertThat(organizationIdentifier).isEqualTo(ORGANIZATION_ID_VALUE);
  }

  @Test
  void getUserIdentifier() {
    String userIdentifier = AuthContext.getUserIdentifier();

    assertThat(userIdentifier).isEqualTo(USER_ID_VALUE);
  }

  @Test
  void getApplicationId() {
    String applicationId = AuthContext.getApplicationId();

    assertThat(applicationId).isEqualTo(X_APPLICATION_ID_VALUE);
  }

  @Test
  void getTenantId() {
    Long tenantId = AuthContext.getTenant();

    assertThat(tenantId).isEqualTo(X_TENANT_ID_VALUE);
  }

  private static @NotNull TokenAuthenticationDetails createTokenAuthenticationDetails() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(Header.X_TENANT_ID, X_TENANT_ID_VALUE);
    request.addHeader(Header.X_APPLICATION_ID, X_APPLICATION_ID_VALUE);
    return new TokenAuthenticationDetails(request);
  }
}
