/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.repository;

import fr.xelians.esafe.organization.entity.OrganizationDb;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationDb, Long> {

  Optional<OrganizationDb> findByIdentifier(String identifier);

  Optional<OrganizationDb> findByTenant(Long tenant);

  boolean existsByIdentifier(String identifier);
}
