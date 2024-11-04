/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import fr.xelians.esafe.storage.domain.offer.StorageClass;

/*
 * @author Emmanuel Deviller
 */
public enum S3Provider {
  AWS,
  MINIO,
  SCALEWAY,
  SCALITY;

  private static final StorageClassDetails S3_DEFAULT_DETAILS =
      new StorageClassDetails("STANDARD", "STANDARD", false, -1);

  public StorageClassDetails getS3StorageClassDetails(StorageClass storageClass) {
    return switch (this) {
      case AWS -> AwsS3Storage.getStorageClassDetails(storageClass);
      case SCALEWAY -> ScalewayS3Storage.getStorageClassDetails(storageClass);
      default -> S3_DEFAULT_DETAILS;
    };
  }
}
