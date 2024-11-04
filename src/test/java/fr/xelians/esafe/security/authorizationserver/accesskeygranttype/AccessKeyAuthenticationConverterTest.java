package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

class AccessKeyAuthenticationConverterTest {

  private static final String SCOPE_1 = "scope-1";

  private static final String ACCESS_KEY_1 = "access-key-1";

  private static final String INVALID_GRANT_TYPE = "not-access-key-grant-type";

  private final OAuth2AccessKeyAuthenticationConverter converter =
      new OAuth2AccessKeyAuthenticationConverter();

  @Test
  void convertWhenHttpMethodNotPostThenReturnNull() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(HttpMethod.GET.name());

    Authentication authentication = converter.convert(request);

    assertThat(authentication).isNull();
  }

  @Test
  void convertWhenGrantTypeNotAccessKeyThenReturnNull() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(HttpMethod.POST.name());
    request.addParameter(OAuth2ParameterNames.GRANT_TYPE, INVALID_GRANT_TYPE);

    Authentication authentication = converter.convert(request);

    assertThat(authentication).isNull();
  }

  @Test
  void convertWhenMultipleAccessKeysThenInvalidRequestError() {
    MockHttpServletRequest request = createAccessKeyGrantTypeRequest();
    request.addParameter(OAuth2AccessKeyParameterNames.ACCESS_KEY, ACCESS_KEY_1);
    request.addParameter(OAuth2AccessKeyParameterNames.ACCESS_KEY, "access-key-2");

    assertThatExceptionOfType(OAuth2AuthenticationException.class)
        .isThrownBy(() -> converter.convert(request))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .extracting(OAuth2AuthenticationException::getError)
        .extracting(OAuth2Error::getErrorCode)
        .isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
  }

  @Test
  void convertWhenMissingAccessKeyThenInvalidRequestError() {
    MockHttpServletRequest request = createAccessKeyGrantTypeRequest();

    assertThatExceptionOfType(OAuth2AuthenticationException.class)
        .isThrownBy(() -> converter.convert(request))
        .extracting(OAuth2AuthenticationException::getError)
        .extracting(OAuth2Error::getErrorCode)
        .isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
  }

  @Test
  void convertWhenMultipleScopesThenInvalidRequestError() {
    MockHttpServletRequest request = createAccessKeyGrantTypeRequest();
    request.addParameter(OAuth2AccessKeyParameterNames.ACCESS_KEY, ACCESS_KEY_1);
    request.addParameter(OAuth2ParameterNames.SCOPE, SCOPE_1);
    request.addParameter(OAuth2ParameterNames.SCOPE, "scope-2");

    assertThatExceptionOfType(OAuth2AuthenticationException.class)
        .isThrownBy(() -> converter.convert(request))
        .extracting(OAuth2AuthenticationException::getError)
        .extracting(OAuth2Error::getErrorCode)
        .isEqualTo(OAuth2ErrorCodes.INVALID_REQUEST);
  }

  @Test
  void convertWhenAccessKeyWithCustomParametersThenAdditionalParametersIncluded() {
    MockHttpServletRequest request = createAccessKeyGrantTypeRequest();
    request.addParameter(OAuth2ParameterNames.SCOPE, SCOPE_1);
    request.addParameter(OAuth2AccessKeyParameterNames.ACCESS_KEY, ACCESS_KEY_1);
    String name = "custom-param";
    String customValueOne = "custom-value-1";
    String customValueTwo = "custom-value-2";
    request.addParameter(name, customValueOne, customValueTwo);
    mockClientAuthentication();

    OAuth2AccessKeyRequestAuthenticationToken authentication =
        (OAuth2AccessKeyRequestAuthenticationToken) this.converter.convert(request);

    assertThat(authentication)
        .isNotNull()
        .extracting(OAuth2AuthorizationGrantAuthenticationToken::getPrincipal)
        .isInstanceOf(TestingAuthenticationToken.class);
    assertThat(authentication.getScopes()).isEqualTo(Set.of(SCOPE_1));
    assertThat(authentication.getGrantType().getValue())
        .isEqualTo(OAuth2AccessKeyParameterNames.GRANT_TYPE);
    assertThat(authentication.getAccessKey()).isEqualTo(ACCESS_KEY_1);
    assertThat(authentication.getAdditionalParameters())
        .containsOnly(entry(name, new String[] {customValueOne, customValueTwo}));
  }

  private static MockHttpServletRequest createAccessKeyGrantTypeRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setMethod(HttpMethod.POST.name());
    request.addParameter(OAuth2ParameterNames.GRANT_TYPE, OAuth2AccessKeyParameterNames.GRANT_TYPE);
    return request;
  }

  private static void mockClientAuthentication() {
    SecurityContextImpl securityContext = new SecurityContextImpl();
    securityContext.setAuthentication(new TestingAuthenticationToken("client-1", null));
    SecurityContextHolder.setContext(securityContext);
  }
}
