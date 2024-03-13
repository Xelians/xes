/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
