/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.repository;

import fr.xelians.esafe.organization.entity.UserDb;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/*
 * @author Emmanuel Deviller
 */
public interface UserRepository extends JpaRepository<UserDb, Long> {

  @Query("SELECT u FROM UserDb u JOIN FETCH u.organization org where u.username =:username")
  Optional<UserDb> findByUsername(String username);

  @Query(
      "SELECT u FROM UserDb u JOIN FETCH u.organization org WHERE u.identifier = :userIdentifier and org.identifier = :organizationIdentifier")
  Optional<UserDb> findByIdentifierAndOrganizationIdentifier(
      @Param("userIdentifier") String userIdentifier,
      @Param("organizationIdentifier") String organizationIdentifier);

  List<UserDb> findByOrganizationId(Long organizationId);

  List<UserDb> findByOrganizationIdentifier(String organizationIdentifier);
}
