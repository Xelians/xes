/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver.accesskeygranttype;

import org.springframework.security.oauth2.core.AuthorizationGrantType;

/*
 * @author Youcef Bouhaddouza
 */
public final class OAuth2AccessKeyParameterNames {

  public static final String GRANT_TYPE = "access_key";

  public static final String ACCESS_KEY = "access_key";

  public static final AuthorizationGrantType ACCESS_KEY_AUTHORIZATION_GRANT_TYPE =
      new AuthorizationGrantType(GRANT_TYPE);

  private OAuth2AccessKeyParameterNames() {}
}
