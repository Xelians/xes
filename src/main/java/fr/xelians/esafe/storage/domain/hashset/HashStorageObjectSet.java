/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.hashset;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.ListIterator;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.HashStorageObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class HashStorageObjectSet implements StorageObjectSet {

  public static final int LIST_SIZE = 50_000;

  private final Map<CharSequence, byte[]> storageObjects;

  public HashStorageObjectSet(int capacity) {
    storageObjects = HashMap.newHashMap(capacity);
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

  @Override
  public Iterator<HashStorageObject> iterator() {
    return new HashStorageObjectIterator(storageObjects);
  }

  @Override
  public void close() {
    // Do nothing
  }

  @Override
  public Iterator<List<HashStorageObject>> listIterator() {
    return ListIterator.iterator(this, LIST_SIZE);
  }

  private static class HashStorageObjectIterator implements Iterator<HashStorageObject> {

    private final Iterator<Entry<CharSequence, byte[]>> iterator;

    public HashStorageObjectIterator(Map<CharSequence, byte[]> storageObjects) {
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
          Hash.VALUES[bytes[0]],
          checksum);
    }
  }
}
