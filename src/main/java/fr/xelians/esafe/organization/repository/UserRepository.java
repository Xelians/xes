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

public interface UserRepository extends JpaRepository<UserDb, Long> {

  // @Query("select u from UserDb u left join fetch u.grants where u.username =:username")
  // Optional<UserDb> findOneByUsername(@Param("username") String username);
  Optional<UserDb> findByUsername(String username);

  List<UserDb> findAll();

  Optional<UserDb> findByIdentifierAndOrganizationId(String identifier, Long organizationId);

  List<UserDb> findByOrganizationId(Long organizationId);

  //    List<UserDb> findAllByOrganization(OrganizationDb organization);

  //    @Query("select userDb from UserDb userDb left join fetch userDb.organization where
  // userDb.identifier =:identifier and userDb.organization.id =:organizationId"  )
  //    Optional<UserDb> findOneByIdentifierAndByOrganization(@Param("identifier") String
  // identifier, @Param("organizationId") Long organizationId);

  //    Optional<UserDb> findOneByEmail(String email);
  //
  //    boolean existsByUsername(String name);
  //
  //    boolean existsByEmail(String email);

  //    @Query("select userDb from UserDb userDb left join fetch userDb.organization where userDb.id
  // =:id")
  //    Optional<UserDb> findOneById(@Param("id") long id);

  //    @Query("select userDb from UserDb userDb left join fetch userDb.organization where
  // userDb.identifier =:identifier")
  //    Optional<UserDb> findOneByIdentifier(@Param("identifier") String identifier);

  //    @Query("select userDb from UserDb userDb left join fetch userDb.organization where
  // userDb.organization.id =:organizationId")
  //    List<UserDb> findAllByOrganizationId(@Param("organizationId") Long organizationId);

}
