/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.domain.ingest;

import fr.xelians.esafe.archive.domain.ingest.AbstractManifestParser;
import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.ManagementMetadata;
import fr.xelians.esafe.archive.domain.unit.object.DataObjectGroup;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class DummyParser extends AbstractManifestParser {

  protected DummyParser() {
    super();
  }

  @Override
  public void parse(String sedaVersion, Path manifestPath, Path sipDir) throws IOException {
    throw new IOException("!!");
  }

  public void callCheckBinarySize(Path binaryPath, long size) throws IOException {
    super.checkBinarySize(binaryPath, size);
  }

  @Override
  public ArchiveTransfer getArchiveTransfert() {
    return null;
  }

  @Override
  public List<DataObjectGroup> getDataObjectGroups() {
    return null;
  }

  @Override
  public List<ArchiveUnit> getArchiveUnits() {
    return null;
  }

  @Override
  public ManagementMetadata getManagementMetadata() {
    return null;
  }
}
