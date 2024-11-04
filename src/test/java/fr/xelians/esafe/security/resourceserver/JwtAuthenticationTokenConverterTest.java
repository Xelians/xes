package fr.xelians.esafe.security.resourceserver;

import static fr.xelians.esafe.security.resourceserver.JwtTestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtAuthenticationTokenConverterTest {

  private final JwtAuthenticationTokenConverter converter = new JwtAuthenticationTokenConverter();

  @Test
  void convertWhenTenantAndGlobalRolesThenMapToDifferentGrantedAuthorities() {

    JwtAuthenticationToken jwtAuthenticationToken = converter.convert(JWT);

    assertThat(jwtAuthenticationToken)
        .isNotNull()
        .extracting(JwtAuthenticationToken::getToken)
        .isNotNull()
        .isEqualTo(JWT);

    assertThat(jwtAuthenticationToken.getAuthorities())
        .containsExactlyInAnyOrder(
            TENANT_ONE_GRANTED_AUTHORITY, TENANT_TWO_GRANTED_AUTHORITY, GLOBAL_GRANTED_AUTHORITY);
  }
}
