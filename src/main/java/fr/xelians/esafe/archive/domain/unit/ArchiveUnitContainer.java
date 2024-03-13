/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import java.util.List;

public interface ArchiveUnitContainer {

  void addArchiveUnit(ArchiveUnit archiveUnit);

  boolean removeArchiveUnit(ArchiveUnit archiveUnit);

  List<ArchiveUnit> getArchiveUnits();
}
