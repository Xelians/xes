/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract sealed class StorageObject extends StorageObjectId
    permits PathStorageObject, ByteStorageObject {

  private final boolean ignoreChecksum;
  private final boolean ignoreEncryption;

  protected StorageObject(
      Long id, StorageObjectType type, boolean ignoreChecksum, boolean ignoreEncryption) {
    super(id, type);
    this.ignoreChecksum = ignoreChecksum;
    this.ignoreEncryption = ignoreEncryption;
  }
}
