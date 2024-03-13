/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.performancetest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class PeInit implements BeforeAllCallback {

  // Ressources
  public static final String RESOURCES = "src/test/resources/";
  public static final String REFERENTIEL = RESOURCES + "/referentiel/";
  public static final String RULE = REFERENTIEL + "rule/";
  public static final String AGENCY = REFERENTIEL + "agency/";
  public static final String ONTOLOGY = REFERENTIEL + "ontology/";
  public static final String PROFILE = REFERENTIEL + "profile/";
  public static final String INGESTCONTRACT = REFERENTIEL + "ingestcontract/";
  public static final String SEDA_SIP = RESOURCES + "sedav2/sip/";
  public static final String SEDA_FILING = RESOURCES + "sedav2/filing/";
  public static final String SEDA_HOLDING = RESOURCES + "sedav2/holding/";
  public static final String PDF = RESOURCES + "/pdf/";
  public static final String RESULTS = "target/integrationtest-results/";

  // Init Tests containers
  static {
    LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Path testDir = Paths.get(RESULTS);
    Files.createDirectories(testDir);
  }
}
