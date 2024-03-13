/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.domain.ingest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.testcommon.TestInit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TestInit.class)
class AbstractManifestParserTest {

  private final Path binaryPath = Paths.get(TestInit.RESOURCES + "sedav2/sip/deepsip_seda.zip");
  private final Path emptyPath = Paths.get("");

  @Test
  void checkArchiveUnitId() {}

  @Test
  void testCheckArchiveUnitId() {}

  @Test
  void checkAgency() {}

  @Test
  void checkArchivalAgreement() {}

  @Test
  void checkManagementRules() {}

  @Test
  void checkArchivalProfile() {}

  @Test
  void checkMaxArchiveUnits() {}

  @Test
  void checkBinary() {}

  @Test
  void checkBinarySize() throws IOException {
    long size = Files.size(binaryPath);

    DummyParser parser = new DummyParser();

    assertThrows(EsafeException.class, () -> parser.callCheckBinarySize(binaryPath, size + 1));
    assertThrows(EsafeException.class, () -> parser.callCheckBinarySize(emptyPath, size));
    assertDoesNotThrow(() -> parser.callCheckBinarySize(binaryPath, size));
  }

  @Test
  void checkBinaryDigest() {}

  @Test
  void checkBinaryFormat() {}

  @Test
  void checkPhysicalQualifier() {}

  @Test
  void checkBinaryQualifier() {}
}
