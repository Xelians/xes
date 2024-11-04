package fr.xelians.esafe.security.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import fr.xelians.esafe.integrationtest.BaseIT;
import fr.xelians.esafe.security.authorizationserver.accesskeygranttype.AccessKeyAuthentication;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class AccessKeyAuthenticationProviderIT extends BaseIT {

  private static final String INVALID_ACCESS_KEY = "invalid-access-key";

  @Autowired
  @Qualifier("accessKeyAuthenticationProvider")
  private AuthenticationProvider authenticationProvider;

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void authenticateUserWhenAccessKeyNotValidThenThrowException() {
    AccessKeyAuthentication unauthenticated =
        AccessKeyAuthentication.unauthenticated(INVALID_ACCESS_KEY);

    assertThatExceptionOfType(InvalidBearerTokenException.class)
        .isThrownBy(() -> authenticationProvider.authenticate(unauthenticated))
        .withMessage("An error occurred while attempting to decode the Jwt: Malformed token");
  }

  @Test
  void authenticateUserWhenAccessKeyValidThenUserAuthenticated() {
    AccessKeyAuthentication unauthenticated =
        AccessKeyAuthentication.unauthenticated(restClient.getAccessKey());

    Authentication authentication = authenticationProvider.authenticate(unauthenticated);

    assertThat(authentication).isNotNull();
    assertThat(authentication.isAuthenticated()).isTrue();
  }
}
