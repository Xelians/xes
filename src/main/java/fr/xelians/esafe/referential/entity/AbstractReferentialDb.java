/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractReferentialDb extends AbstractBaseDb implements ReferentialDb {

  @Id
  @SequenceGenerator(name = "referential_generator", sequenceName = "referential_seq")
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "referential_generator")
  protected Long id;

  @Column(nullable = false, updatable = false, length = 64)
  @NotNull
  @Length(min = 1, max = 64)
  protected String identifier;

  @NotNull
  @Min(value = 0)
  @Column(nullable = false, updatable = false)
  protected Long tenant;
}
