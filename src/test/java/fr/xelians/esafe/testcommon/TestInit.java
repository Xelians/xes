/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The type Test init.
 *
 * @author Emmanuel Deviller
 */
public class TestInit implements BeforeAllCallback {

  public static final String RESOURCES = "src/test/resources/";

  public static final String RESULTS = "target/test-results/";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Path testDir = Paths.get(RESULTS);
    Files.createDirectories(testDir);
  }
}
