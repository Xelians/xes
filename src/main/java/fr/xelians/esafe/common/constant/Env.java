/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.constant;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.NioUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * @author Emmanuel Deviller
 */
public final class Env {

  public static final Path APP_PATH = Path.of("/tmp/esafe");

  public static final Path DATA_PATH = APP_PATH.resolve("data");
  public static final Path LOG_PATH = APP_PATH.resolve("log");
  public static final Path TMP_PATH = APP_PATH.resolve("tmp");
  public static final Path CONFIG_PATH = APP_PATH.resolve("cnf");

  public static final Path OFFER_PATH = DATA_PATH.resolve("offers");
  public static final Path OPERATION_PATH = DATA_PATH.resolve("operations");
  public static final Path INGEST_PATH = DATA_PATH.resolve("ingests");

  // Todo get root folder from properties
  public static final String MANIFEST_XML = "manifest.xml";

  static {
    try {
      // Clean TMP dir
      NioUtils.deleteDirQuietly(TMP_PATH);

      // Build all dirs
      Files.createDirectories(DATA_PATH);
      Files.createDirectories(LOG_PATH);
      Files.createDirectories(TMP_PATH);
      Files.createDirectories(CONFIG_PATH);
      Files.createDirectories(INGEST_PATH);
      Files.createDirectories(OPERATION_PATH);
      Files.createDirectories(OFFER_PATH);
    } catch (IOException ex) {
      throw new InternalException(
          "Failed to init application environnement", "Failed to create default directories", ex);
    }
  }

  private Env() {}
}
