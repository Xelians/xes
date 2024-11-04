/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.storage.domain.StorageInputStream;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.StorageType;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface StorageOffer {

  String getName();

  StorageType getStorageType();

  boolean isActive();

  boolean isEncrypted();

  List<byte[]> getStorageObjectBytes(Long tenant, List<StorageObjectId> storageObjectIds)
      throws IOException;

  byte[] getStorageObjectByte(Long tenant, Long id, StorageObjectType type) throws IOException;

  StorageInputStream getStorageObjectStream(Long tenant, Long id, StorageObjectType type)
      throws IOException;

  StorageInputStream getStorageObjectStream(
      Long tenant, Long id, long start, long end, StorageObjectType type) throws IOException;

  void putStorageObjects(Long tenant, List<StorageObject> storageObjects) throws IOException;

  void putStorageObject(Path srcPath, Long tenant, Long id, StorageObjectType type)
      throws IOException;

  void putStorageObject(byte[] bytes, Long tenant, Long id, StorageObjectType type)
      throws IOException;

  void putStorageObject(InputStream inputStream, Long tenant, Long id, StorageObjectType type)
      throws IOException;

  void deleteStorageObject(Long tenant, Long id, StorageObjectType type) throws IOException;

  void deleteStorageObjectIfExists(Long tenant, Long id, StorageObjectType type) throws IOException;

  boolean existsStorageObject(Long tenant, Long id, StorageObjectType type) throws IOException;

  List<Long> findStorageObjectIdsByType(Long tenant, StorageObjectType type) throws IOException;

  byte[] writeLbk(Long tenant, List<OperationDb> operations, long secureNum, Hash hash)
      throws IOException;

  void close();
}
