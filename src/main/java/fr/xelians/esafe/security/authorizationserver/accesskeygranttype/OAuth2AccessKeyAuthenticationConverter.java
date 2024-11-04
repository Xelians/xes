/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Component responsible for converting incoming authentication requests into {@link
 * OAuth2AccessKeyRequestAuthenticationToken } that Spring Security can understand and use for
 * authentication. It is inspired by {@link
 * org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider}
 *
 * @author Youcef Bouhaddouza
 */
public class OAuth2AccessKeyAuthenticationConverter implements AuthenticationConverter {
  private static final String ACCESS_TOKEN_REQUEST_ERROR_URI =
      "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2";

  @Nullable
  @Override
  public Authentication convert(HttpServletRequest request) {

    if (isHttpMethodAllowed(request)) return null;

    MultiValueMap<String, String> parameters = getFormParameters(request);

    // grant_type (REQUIRED)
    if (isAccessKeyGrantType(parameters)) return null;

    Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

    // access_key (REQUIRED)
    String accessKey = getAccessKeyElseThrowInvalidRequest(parameters);

    // scope (OPTIONAL)
    Set<String> scopes = getScopesElseThrowInvalidRequest(parameters);

    // additionalParameters (OPTIONAL)
    Map<String, Object> additionalParameters = getAdditionalParameters(parameters);

    return new OAuth2AccessKeyRequestAuthenticationToken(
        accessKey, clientPrincipal, scopes, additionalParameters);
  }

  private boolean isHttpMethodAllowed(HttpServletRequest request) {
    return !HttpMethod.POST.name().equals(request.getMethod());
  }

  private boolean isAccessKeyGrantType(MultiValueMap<String, String> parameters) {
    String grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE);
    return !OAuth2AccessKeyParameterNames.GRANT_TYPE.equals(grantType);
  }

  private String getAccessKeyElseThrowInvalidRequest(MultiValueMap<String, String> parameters) {
    String accessKey = parameters.getFirst(OAuth2AccessKeyParameterNames.ACCESS_KEY);
    if (!StringUtils.hasText(accessKey)
        || parameters.get(OAuth2AccessKeyParameterNames.ACCESS_KEY).size() != 1) {
      throwError(
          OAuth2ErrorCodes.INVALID_REQUEST,
          OAuth2AccessKeyParameterNames.ACCESS_KEY,
          ACCESS_TOKEN_REQUEST_ERROR_URI);
    }
    return accessKey;
  }

  private Set<String> getScopesElseThrowInvalidRequest(MultiValueMap<String, String> parameters) {
    String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
    if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
      throwError(
          OAuth2ErrorCodes.INVALID_REQUEST,
          OAuth2ParameterNames.SCOPE,
          ACCESS_TOKEN_REQUEST_ERROR_URI);
    }
    Set<String> scopes = null;
    if (StringUtils.hasText(scope)) {
      scopes = new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }
    return scopes;
  }

  private Map<String, Object> getAdditionalParameters(MultiValueMap<String, String> parameters) {
    Map<String, Object> additionalParameters = new HashMap<>();
    parameters.forEach(
        (key, value) -> {
          if (!key.equals(OAuth2ParameterNames.GRANT_TYPE)
              && !key.equals(OAuth2ParameterNames.CLIENT_ID)
              && !key.equals(OAuth2ParameterNames.CLIENT_SECRET)
              && !key.equals(OAuth2ParameterNames.SCOPE)
              && !key.equals(OAuth2AccessKeyParameterNames.ACCESS_KEY)) {
            additionalParameters.put(
                key, (value.size() == 1) ? value.getFirst() : value.toArray(new String[0]));
          }
        });
    return additionalParameters;
  }

  private static void throwError(String errorCode, String parameterName, String errorUri) {
    OAuth2Error error =
        new OAuth2Error(errorCode, "OAuth 2.0 Parameter: " + parameterName, errorUri);
    throw new OAuth2AuthenticationException(error);
  }

  private static MultiValueMap<String, String> getFormParameters(HttpServletRequest request) {
    Map<String, String[]> parameterMap = request.getParameterMap();
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameterMap.forEach(
        (key, values) -> {
          String queryString =
              StringUtils.hasText(request.getQueryString()) ? request.getQueryString() : "";
          // If not query parameter then it's a form parameter
          if (!queryString.contains(key)) {
            for (String value : values) {
              parameters.add(key, value);
            }
          }
        });
    return parameters;
  }
}
