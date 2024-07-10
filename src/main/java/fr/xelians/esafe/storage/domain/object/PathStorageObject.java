/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import java.nio.file.Path;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public final class PathStorageObject extends StorageObject {

  private final Path path;

  public PathStorageObject(Path path, Long id, StorageObjectType type) {
    this(path, id, type, false, false);
  }

  public PathStorageObject(Path path, Long id, StorageObjectType type, boolean ignoreChecksum) {
    this(path, id, type, ignoreChecksum, false);
  }

  public PathStorageObject(
      Path path,
      Long id,
      StorageObjectType type,
      boolean ignoreChecksum,
      boolean ignoreEncryption) {
    super(id, type, ignoreChecksum, ignoreEncryption);
    Assert.notNull(path, "Path cannot be null");
    this.path = path;
  }
}
