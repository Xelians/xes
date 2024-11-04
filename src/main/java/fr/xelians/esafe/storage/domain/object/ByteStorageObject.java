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
import org.springframework.util.Assert;

/*
 * @author Emmanuel Deviller
 */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class ByteStorageObject extends StorageObject {

  private final byte[] bytes;

  public ByteStorageObject(byte[] bytes, Long id, StorageObjectType type) {
    this(bytes, id, type, false, false);
  }

  public ByteStorageObject(byte[] bytes, Long id, StorageObjectType type, boolean ignoreChecksum) {
    this(bytes, id, type, ignoreChecksum, false);
  }

  public ByteStorageObject(
      byte[] bytes,
      Long id,
      StorageObjectType type,
      boolean ignoreChecksum,
      boolean ignoreEncryption) {
    super(id, type, ignoreChecksum, ignoreEncryption);
    Assert.notNull(bytes, "Bytes cannot be null");
    this.bytes = bytes;
  }

  public long getSize() {
    return bytes.length;
  }
}
