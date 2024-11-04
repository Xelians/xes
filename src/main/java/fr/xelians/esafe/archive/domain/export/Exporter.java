/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.export;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface Exporter {

  void exportDip(List<ArchiveUnit> srcUnits, Path outpath) throws IOException;

  void exportSip(List<ArchiveUnit> srcUnits, Path outpath) throws IOException;
}
