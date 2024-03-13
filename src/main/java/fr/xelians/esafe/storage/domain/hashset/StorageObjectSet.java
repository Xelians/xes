/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.hashset;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.HashStorageObject;
import java.util.Iterator;
import java.util.List;

public interface StorageObjectSet extends Iterable<HashStorageObject>, AutoCloseable {

  void add(long id, StorageObjectType type, Hash hash, byte[] checksum);

  void remove(long id, StorageObjectType type);

  long size();

  void close();

  Iterator<List<HashStorageObject>> listIterator();
}
