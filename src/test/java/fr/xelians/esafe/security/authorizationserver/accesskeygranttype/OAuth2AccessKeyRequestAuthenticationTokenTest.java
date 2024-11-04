package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import static fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyParameterNames.ACCESS_KEY_AUTHORIZATION_GRANT_TYPE;
import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class OAuth2AccessKeyRequestAuthenticationTokenTest {

  private static final String SCOPE_1 = "scope-1";

  private static final String ACCESS_KEY_1 = "access-key-1";

  private static final Map<String, Object> ADDITIONAL_PARAMETERS = Map.of("param1", "value1");

  private final RegisteredClient REGISTERED_CLIENT = registeredClient().build();

  private final OAuth2ClientAuthenticationToken CLIENT_PRINCIPAL =
      new OAuth2ClientAuthenticationToken(
          REGISTERED_CLIENT,
          ClientAuthenticationMethod.CLIENT_SECRET_POST,
          REGISTERED_CLIENT.getClientSecret());

  @Test
  void constructorWhenAccessTokenNullThenThrowIllegalArgumentException() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                new OAuth2AccessKeyRequestAuthenticationToken(
                    null, CLIENT_PRINCIPAL, Set.of(SCOPE_1), null))
        .withMessage("accessKey cannot be empty");
  }

  @Test
  void constructorWhenClientPrincipalNullThenThrowIllegalArgumentException() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                new OAuth2AccessKeyRequestAuthenticationToken(
                    ACCESS_KEY_1, null, Set.of(SCOPE_1), null))
        .isInstanceOf(IllegalArgumentException.class)
        .withMessage("clientPrincipal cannot be null");
  }

  @Test
  void constructorWhenAllArgumentsProvidedThenCreated() {
    OAuth2AccessKeyRequestAuthenticationToken authentication =
        new OAuth2AccessKeyRequestAuthenticationToken(
            ACCESS_KEY_1, CLIENT_PRINCIPAL, Set.of(SCOPE_1), ADDITIONAL_PARAMETERS);

    assertThat(authentication.getGrantType()).isEqualTo(ACCESS_KEY_AUTHORIZATION_GRANT_TYPE);
    assertThat(authentication.getPrincipal()).isEqualTo(this.CLIENT_PRINCIPAL);
    assertThat(authentication.getCredentials().toString()).isEmpty();
    assertThat(authentication.getAccessKey()).isEqualTo(ACCESS_KEY_1);
    assertThat(authentication.getScopes()).isEqualTo(Set.of(SCOPE_1));
    assertThat(authentication.getAdditionalParameters()).isEqualTo(ADDITIONAL_PARAMETERS);
  }

  private RegisteredClient.Builder registeredClient() {
    return RegisteredClient.withId("registration-1")
        .clientId("client-1")
        .clientIdIssuedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
        .clientSecret("secret-1")
        .authorizationGrantType(
            new AuthorizationGrantType(OAuth2AccessKeyParameterNames.GRANT_TYPE))
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .scope(SCOPE_1);
  }
}
