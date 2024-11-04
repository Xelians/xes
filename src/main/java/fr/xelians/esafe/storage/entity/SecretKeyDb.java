/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(name = "secret_key")
public class SecretKeyDb {

  @Id private Long tenant;

  // The byte[] type is natively supported by JPA
  @NotNull private byte[] secret;
}
