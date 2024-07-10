/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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
