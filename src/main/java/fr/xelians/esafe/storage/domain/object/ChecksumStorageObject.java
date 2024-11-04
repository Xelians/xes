/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ChecksumStorageObject extends StorageObjectId {

  private final Hash hash;
  private final byte[] checksum;

  public ChecksumStorageObject(Long id, StorageObjectType type, Hash hash, byte[] checksum) {
    super(id, type);
    this.hash = hash;
    this.checksum = checksum;
  }
}
