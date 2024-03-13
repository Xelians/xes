/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.dao;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.referential.entity.AgencyDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.ChecksumStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.domain.pack.Packer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface StorageDao extends AutoCloseable {

  List<AgencyDb> getAgencies(Long tenant, List<String> offers) throws IOException;

  List<ArchiveUnit> getArchiveUnits(Long tenant, List<String> offers, Long operationId)
      throws IOException;

  List<ArchiveUnit> getArchiveUnits(Long tenant, List<String> offers, List<Long> operationIds)
      throws IOException;

  InputStream getManifestStream(Long tenant, List<String> offers, Long id) throws IOException;

  InputStream getAtrStream(Long tenant, List<String> offers, Long id) throws IOException;

  InputStream getReportStream(Long tenant, List<String> offers, Long id) throws IOException;

  InputStream getDipStream(Long tenant, List<String> offers, Long id) throws IOException;

  InputStream getBinaryObjectStream(
      Long tenant, List<String> offers, Long operationId, long[] pos, Long id) throws IOException;

  InputStream getAusStream(Long tenant, List<String> offers, Long id) throws IOException;

  InputStream getObjectStream(Long tenant, List<String> offers, Long id, StorageObjectType type)
      throws IOException;

  InputStream getObjectStream(
      Long tenant, List<String> offers, Long id, long start, long end, StorageObjectType type)
      throws IOException;

  byte[] getObjectByte(Long tenant, List<String> offers, Long id, StorageObjectType type)
      throws IOException;

  List<byte[]> getObjectBytes(
      Long tenant, List<String> offers, List<StorageObjectId> storageObjectIds) throws IOException;

  List<ChecksumStorageObject> putStorageObjects(
      Long tenant, List<String> offers, List<StorageObject> storageObjects) throws IOException;

  Packer createPacker(Path path);

  @Override
  void close();
}
