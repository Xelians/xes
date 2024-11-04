/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(name = "storage")
public class StorageDb {

  @Id @NotNull private Long id;

  @Min(value = -1)
  @NotNull
  private Long secureNumber = -1L;

  public long incSecureNumber() {
    return ++secureNumber;
  }

  // Needed by JPA
  public StorageDb() {}

  public StorageDb(Long tenant) {
    this.id = tenant;
  }
}
