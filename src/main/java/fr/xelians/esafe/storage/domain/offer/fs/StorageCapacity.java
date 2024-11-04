/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import lombok.Getter;

/*
 * @author Emmanuel Deviller
 */
@Getter
public enum StorageCapacity {
  SMALL("Small Capacity"),
  MEDIUM("Medium Capacity"),
  LARGE("Large Capacity");

  private final String desc;

  StorageCapacity(String desc) {
    this.desc = desc;
  }
}
