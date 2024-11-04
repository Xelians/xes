/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security;

import lombok.experimental.UtilityClass;

/*
 * @author Youcef Bouhaddouza
 */
@UtilityClass
public class JwtClaimNames {
  public static final String ORGANIZATION_ID = "organization_id";

  public static final String USER_ID = "user_id";

  public static final String ROLES = "roles";

  public static final String GLOBAL_ROLES = "global";

  public static final String TENANT_ROLES = "tenant";
}
