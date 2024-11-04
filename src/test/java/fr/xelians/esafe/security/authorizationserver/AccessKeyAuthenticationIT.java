package fr.xelians.esafe.security.authorizationserver;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.GRANT_TYPE;
import static org.springframework.security.oauth2.server.authorization.oidc.OidcClientMetadataClaimNames.CLIENT_ID;
import static org.springframework.security.oauth2.server.authorization.oidc.OidcClientMetadataClaimNames.CLIENT_SECRET;

import fr.xelians.esafe.integrationtest.BaseIT;
import fr.xelians.esafe.organization.dto.AccessDto;
import fr.xelians.esafe.testcommon.AuthFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

class AccessKeyAuthenticationIT extends BaseIT {

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void authenticateWhenAccessKeyValidThenAuthenticated() {
    ResponseEntity<AccessDto> authenticationResponse = restClient.signIn(restClient.getAccessKey());

    assertEquals(HttpStatus.OK, authenticationResponse.getStatusCode());
    assertThat(authenticationResponse.getBody())
        .satisfies(
            auth -> {
              assertThat(auth.getAccessToken()).isNotBlank();
              assertThat(auth.getRefreshToken()).isNotBlank();
              assertThat(auth.getTokenType()).isEqualTo("Bearer");
            });
  }

  @Test
  void authenticateWhenClientIdInvalidThenThrowException() {
    MultiValueMap<String, String> params =
        AuthFactory.createAccessTokenQueryParams(restClient.getAccessKey());
    params.set(CLIENT_ID, "invalid-client-id");

    Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
        .isThrownBy(() -> restClient.signIn(params))
        .extracting(HttpClientErrorException::getStatusCode)
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void authenticateWhenClientSecretInvalidThenThrowException() {
    MultiValueMap<String, String> params =
        AuthFactory.createAccessTokenQueryParams(restClient.getAccessKey());
    params.set(CLIENT_SECRET, "invalid-client-secret");

    Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
        .isThrownBy(() -> restClient.signIn(params))
        .extracting(HttpClientErrorException::getStatusCode)
        .isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void authenticateWhenGrantTypeInvalidThenThrowException() {
    MultiValueMap<String, String> params =
        AuthFactory.createAccessTokenQueryParams(restClient.getAccessKey());
    params.set(GRANT_TYPE, "invalid-grant-type");

    Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
        .isThrownBy(() -> restClient.signIn(params))
        .extracting(HttpClientErrorException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void authenticateWhenAccessKeyInvalidThenThrowException() {
    Assertions.assertThatExceptionOfType(HttpClientErrorException.class)
        .isThrownBy(() -> restClient.signIn("invalid-access-key"))
        .extracting(HttpClientErrorException::getStatusCode)
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
