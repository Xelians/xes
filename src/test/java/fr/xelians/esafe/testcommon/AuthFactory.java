package fr.xelians.esafe.testcommon;

import static fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyParameterNames.ACCESS_KEY;
import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.*;

import fr.xelians.esafe.security.authorizationserver.accesskeygranttype.OAuth2AccessKeyParameterNames;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class AuthFactory {

  public static final String ACCESS_TOKEN_URI = "/oauth2/token";

  public static final String REFRESH_TOKEN_URI = "/oauth2/token";

  public static final String TOKEN_REVOCATION_URI = "/oauth2/revoke";

  public static HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return headers;
  }

  public static MultiValueMap<String, String> createAccessTokenQueryParams(String accessKey) {
    MultiValueMap<String, String> params = createClientParams();
    params.add(GRANT_TYPE, OAuth2AccessKeyParameterNames.GRANT_TYPE);
    params.add(ACCESS_KEY, accessKey);
    return params;
  }

  public static MultiValueMap<String, String> createRefreshTokenQueryParams(String refreshToken) {
    MultiValueMap<String, String> params = createClientParams();
    params.add(GRANT_TYPE, REFRESH_TOKEN);
    params.add(REFRESH_TOKEN, refreshToken);
    return params;
  }

  public static MultiValueMap<String, String> createAccessTokenRevokeQueryParams(
      String accessToken) {
    MultiValueMap<String, String> params = createClientParams();
    params.add(TOKEN, accessToken);
    return params;
  }

  private static MultiValueMap<String, String> createClientParams() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add(CLIENT_ID, "test-client-id");
    params.add(CLIENT_SECRET, "test-client-secret");
    return params;
  }
}
