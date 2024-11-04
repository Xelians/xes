/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface ArchiveUnitContainer {

  void addArchiveUnit(ArchiveUnit archiveUnit);

  boolean removeArchiveUnit(ArchiveUnit archiveUnit);

  List<ArchiveUnit> getArchiveUnits();
}
