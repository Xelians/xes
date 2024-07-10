/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FileSystemStorage {

  @NotBlank private String name;

  @NotBlank private String root;

  private boolean sync = false;

  private boolean isActive = true;

  private StorageCapacity capacity = StorageCapacity.MEDIUM;

  private String node;

  @Min(1)
  @Max(1024)
  private int concurrency = 16;

  public StorageType getStorageTye() {
    return StorageType.FS;
  }
}
