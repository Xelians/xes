/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.domain;

/*
 * @author Emmanuel Deviller
 */
public class Role {
  /** Global ROLES */
  public enum GlobalRole {
    ROLE_ROOT_ADMIN,
    ROLE_ADMIN,
    ROLE_DEPRECATED;

    public static class Names {
      /**
       * Instance administrator: Root User has access to all the resources (tenants, users, etc.) of
       * all organizations with read and write permissions. Warning: not use the root role to secure
       * the API
       */
      public static final String ROLE_ROOT_ADMIN = "ROLE_ROOT_ADMIN";

      /**
       * Organization administrator: User has access to all the resources (tenants, users, etc.) of
       * his organization with read and write permissions.
       */
      public static final String ROLE_ADMIN = "ROLE_ADMIN";

      /**
       * This role is not assigned to any user. It is only useful for protecting deprecated APIs
       * that are no longer intended for external use.
       */
      public static final String ROLE_DEPRECATED = "ROLE_DEPRECATED";
    }
  }

  /** Tenant ROLES */
  public enum TenantRole {
    ROLE_ARCHIVE_MANAGER,
    ROLE_ARCHIVE_READER;

    public static class Names {

      /** Archive Manager: User has read and write permissions to the relevant tenant */
      public static final String ROLE_ARCHIVE_MANAGER = "ROLE_ARCHIVE_MANAGER";

      /** Archive Reader: User has read permission only to the relevant tenant */
      public static final String ROLE_ARCHIVE_READER = "ROLE_ARCHIVE_READER";
    }
  }
}
