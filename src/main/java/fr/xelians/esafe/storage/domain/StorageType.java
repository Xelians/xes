/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public enum StorageType {
  FS("File System"),
  EFS("Encrypted File System"),
  S3("Amazon S3");

  private final String desc;

  StorageType(String desc) {
    this.desc = desc;
  }
}
