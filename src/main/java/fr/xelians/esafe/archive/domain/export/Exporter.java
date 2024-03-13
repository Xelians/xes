/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.export;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface Exporter {

  void export(List<ArchiveUnit> srcUnits, Path outpath) throws IOException;
}
