/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import static software.amazon.awssdk.services.s3.model.StorageClass.*;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.storage.domain.offer.StorageClass;

/*
 * @author Emmanuel Deviller
 */
public class AwsS3Storage {

  private static final String STANDARD_NAME = STANDARD.toString();

  private AwsS3Storage() {}

  public static StorageClassDetails getStorageClassDetails(StorageClass storageClass) {
    if (storageClass == null) {
      return new StorageClassDetails(STANDARD_NAME, STANDARD_NAME, false, -1);
    } else {
      String primary = getStorageClassName(storageClass.primary());
      String secondary = getStorageClassName(storageClass.secondary());
      long size = secondarySize(secondary);
      boolean restore = needRestore(secondary);
      return new StorageClassDetails(primary, secondary, restore, size);
    }
  }

  private static String getStorageClassName(String storageClassName) {
    if (storageClassName == null) return STANDARD_NAME;

    var awsStorageClass =
        software.amazon.awssdk.services.s3.model.StorageClass.fromValue(storageClassName);

    if (awsStorageClass.equals(UNKNOWN_TO_SDK_VERSION)) {
      throw new InternalException(
          String.format(
              "Failed to init AWS S3 storage with unknown storage class '%s", storageClassName));
    }

    return awsStorageClass.toString();
  }

  private static long secondarySize(String secondary) {
    var awsStorageClass =
        software.amazon.awssdk.services.s3.model.StorageClass.fromValue(secondary);
    return switch (awsStorageClass) {
      case GLACIER_IR, ONEZONE_IA -> 64000; // 64 Ko
      case DEEP_ARCHIVE, GLACIER, INTELLIGENT_TIERING -> 128000; // 128 Ko
      default -> -1;
    };
  }

  private static boolean needRestore(String secondary) {
    var awsStorageClass =
        software.amazon.awssdk.services.s3.model.StorageClass.fromValue(secondary);
    return DEEP_ARCHIVE.equals(awsStorageClass)
        || GLACIER.equals(awsStorageClass)
        || INTELLIGENT_TIERING.equals(awsStorageClass);
  }
}
