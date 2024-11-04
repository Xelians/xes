package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.resourceserver.JwtTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtClaimExtractorTest {

  @Test
  void extractOrganizationId() {
    String organizationIdentifier = JwtClaimExtractor.extractOrganizationId(JWT);

    assertThat(organizationIdentifier).isEqualTo(ORGANIZATION_ID_VALUE);
  }

  @Test
  void extractUserIdentifier() {
    String userIdentifier = JwtClaimExtractor.extractUserIdentifier(JWT);

    assertThat(userIdentifier).isEqualTo(USER_ID_VALUE);
  }

  @Test
  void extractUserGlobalRoles() {
    List<String> globalRoles = JwtClaimExtractor.extractGlobalRoles(JWT);

    assertThat(globalRoles).isEqualTo(GLOBAL_ROLES_VALUES);
  }

  @Test
  void extractUserTenantRoles() {
    Map<String, List<String>> tenantRoles = JwtClaimExtractor.extractTenantRoles(JWT);

    assertThat(tenantRoles).isEqualTo(TENANT_ROLES_VALUES);
  }
}
