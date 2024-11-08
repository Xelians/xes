/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constant;

/*
 * @author Emmanuel Deviller
 */
public final class Header {

  public static final String X_TENANT_ID = "X-Tenant-Id";
  public static final String X_APPLICATION_ID = "X-Application-Id";
  public static final String X_REQUEST_ID = "X-Request-Id";
  public static final String X_CONTEXT_ID = "X-Context-Id";
  public static final String X_ACCESS_CONTRACT_ID = "X-Access-Contract-Id";

  public static final String X_ACCESS_KEY_ID = "X-Access-Key";

  public static final String X_QUALIFIER = "X-Qualifier";
  public static final String X_VERSION = "X-Version";
  public static final String X_FILE = "X-File";
  public static final String X_HTTP_METHOD_OVERRIDE = "X-Http-Method-Override";

  public static final String X_XSRF_TOKEN = "X-XSRF-Token";
  public static final String X_USER_TOKEN = "X-User-Token";
  public static final String X_IDENTITY = "X-Identity";

  public static final String AUTHORIZATION = "Authorization";
  public static final String ACCEPT = "Accept";

  public static final int X_APPLICATION_LEN = 128;

  private Header() {}
}
