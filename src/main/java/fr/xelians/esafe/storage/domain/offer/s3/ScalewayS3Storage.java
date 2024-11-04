/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.storage.domain.offer.StorageClass;

/*
 * @author Emmanuel Deviller
 */
public class ScalewayS3Storage {

  private static final String STANDARD = "STANDARD";
  private static final String ONEZONE_IA = "ONEZONE_IA";
  private static final String GLACIER = "GLACIER";

  private ScalewayS3Storage() {}

  public static StorageClassDetails getStorageClassDetails(StorageClass storageClass) {
    if (storageClass == null) {
      return new StorageClassDetails(STANDARD, STANDARD, false, -1);
    } else {
      String primary = getStorageClassName(storageClass.primary());
      String secondary = getStorageClassName(storageClass.secondary());
      long size = secondarySize(secondary);
      boolean restore = needRestore(secondary);
      return new StorageClassDetails(primary, secondary, restore, size);
    }
  }

  private static String getStorageClassName(String storageClassName) {
    if (storageClassName == null) return STANDARD;

    if (STANDARD.equals(storageClassName)
        || ONEZONE_IA.equals(storageClassName)
        || GLACIER.equals(storageClassName)) return storageClassName;

    throw new InternalException(
        String.format(
            "Failed to init Scaleway S3 storage with unknown storage class '%s", storageClassName));
  }

  private static long secondarySize(String secondary) {
    return switch (secondary) {
      case ONEZONE_IA -> 64000; // 64 Ko
      case GLACIER -> 128000; // 128 Ko
      default -> -1;
    };
  }

  private static boolean needRestore(String secondary) {
    return GLACIER.equals(secondary);
  }
}
