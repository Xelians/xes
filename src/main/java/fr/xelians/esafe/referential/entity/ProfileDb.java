/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.referential.domain.ProfileFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(
    name = "profile",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_profile_tenant_identifier",
          columnNames = {"tenant", "identifier"}),
      @UniqueConstraint(
          name = "unique_profile_tenant_name",
          columnNames = {"tenant", "name"})
    })
public class ProfileDb extends AbstractReferentialDb {

  private ProfileFormat format;

  @Column(length = 262144)
  private byte[] data;
}
