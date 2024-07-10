/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.domain.role;

public final class RoleName {

  private RoleName() {}

  public static final String ROLE_ADMIN = "ROLE_ADMIN";
  public static final String ROLE_ORGA_WRITER = "ROLE_ORGA_WRITER";
  public static final String ROLE_ORGA_READER = "ROLE_ORGA_READER";
  public static final String ROLE_USER_WRITER = "ROLE_USER_WRITER";
  public static final String ROLE_USER_READER = "ROLE_USER_READER";

  public static final String ROLE_REFERENTIAL_WRITER = "ROLE_REFERENTIAL_WRITER";
  public static final String ROLE_REFERENTIAL_READER = "ROLE_REFERENTIAL_READER";
  public static final String ROLE_ARCHIVE_WRITER = "ROLE_ARCHIVE_WRITER";
  public static final String ROLE_ARCHIVE_READER = "ROLE_ARCHIVE_READER";
}
