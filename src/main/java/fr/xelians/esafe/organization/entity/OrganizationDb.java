/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.entity;

import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

/*
 * @author Emmanuel Deviller
 */
@Getter
@Setter
@Entity
@Table(
    name = "organization",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_organization_identifier",
          columnNames = {"identifier"})
    })
public class OrganizationDb extends AbstractBaseDb {

  @Id
  @SequenceGenerator(name = "organization_generator", sequenceName = "organization_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_generator")
  protected Long id;

  @Column(nullable = false, updatable = false)
  @NotNull
  @Length(min = 1, max = 64)
  protected String identifier;

  // This is the default tenant for this organization.
  // The default tenant cannot be modified or removed
  @Min(value = 0)
  protected Long tenant;
}
