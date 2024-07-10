/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "agency",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "unique_agency_tenant_identifier",
          columnNames = {"tenant", "identifier"})
    })
public class AgencyDb extends AbstractReferentialDb {}
