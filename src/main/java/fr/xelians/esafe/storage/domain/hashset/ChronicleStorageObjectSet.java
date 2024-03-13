/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.hashset;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.ListIterator;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.HashStorageObject;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.apache.commons.lang3.Validate;

@Slf4j
public class ChronicleStorageObjectSet implements StorageObjectSet {

  public static final int LIST_SIZE = 50_000;
  protected static final String AVERAGE_KEY = "1234567890.uni";
  protected static final byte[] AVERAGE_VALUE = new byte[17]; // MD5 => 128 bits => 16 bytes

  private final ChronicleMap<CharSequence, byte[]> storageObjects;

  public ChronicleStorageObjectSet(long capacity) throws IOException {
    try {
      storageObjects =
          ChronicleMapBuilder.of(CharSequence.class, byte[].class)
              .name("storage_object_set")
              .averageKey(AVERAGE_KEY)
              .averageValue(AVERAGE_VALUE)
              .entries(capacity)
              .putReturnsNull(true)
              .create();
    } catch (Throwable throwable) {
      throw new IOException(
          "Failed to create ChronicleMap - See https://chronicle.software/chronicle-support-java-17/",
          throwable);
    }
  }

  @Override
  public void add(long id, StorageObjectType type, Hash hash, byte[] checksum) {
    Validate.notNull(type, "type");
    Validate.notNull(hash, "hash");
    Validate.notNull(checksum, "checksum");

    byte[] bytes = new byte[checksum.length + 1];
    bytes[0] = (byte) hash.ordinal();
    System.arraycopy(checksum, 0, bytes, 1, checksum.length);

    storageObjects.put(id + ";" + type, bytes);
  }

  @Override
  public void remove(long id, StorageObjectType type) {
    Validate.notNull(type, "type");

    storageObjects.remove(id + ";" + type);
  }

  @Override
  public long size() {
    return storageObjects.size();
  }

  // Note. This iterator is not thread safe
  @Override
  public Iterator<HashStorageObject> iterator() {
    return new ChronicleStorageObjectIterator(storageObjects);
  }

  @Override
  public Iterator<List<HashStorageObject>> listIterator() {
    return ListIterator.iterator(this, LIST_SIZE);
  }

  @Override
  public void close() {
    if (storageObjects != null) {
      storageObjects.close();
    }
  }

  private static class ChronicleStorageObjectIterator implements Iterator<HashStorageObject> {

    private final Iterator<Entry<CharSequence, byte[]>> iterator;

    public ChronicleStorageObjectIterator(ChronicleMap<CharSequence, byte[]> storageObjects) {
      iterator = storageObjects.entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public HashStorageObject next() {
      Entry<CharSequence, byte[]> entry = iterator.next();

      String key = entry.getKey().toString();
      int kd = key.indexOf(";");

      byte[] bytes = entry.getValue();
      byte[] checksum = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, checksum, 0, checksum.length);

      return HashStorageObject.create(
          Long.parseLong(key.substring(0, kd)),
          StorageObjectType.valueOf(key.substring(kd + 1)),
          Hash.values()[bytes[0]],
          checksum);
    }
  }
}
