/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.xelians.esafe.common.constant.DefaultValue;
import fr.xelians.esafe.common.entity.database.AbstractBaseDb;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenant")
public class TenantDb extends AbstractBaseDb {

  @Id
  @SequenceGenerator(name = "tenant_generator", sequenceName = "tenant_seq", allocationSize = 1)
  @GeneratedValue(generator = "tenant_generator")
  private Long id;

  @NotNull
  @Column(length = 512)
  private ArrayList<String> storageOffers = new ArrayList<>();

  private Boolean encrypted = DefaultValue.ENCRYPTED;

  /** Many tenants could link to the same organization */
  @JsonIgnore
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrganizationDb organization;
}
