/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.authentication.repository;

import fr.xelians.esafe.authentication.entity.RefreshTokenDb;
import fr.xelians.esafe.organization.entity.UserDb;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenDb, Long> {

  Optional<RefreshTokenDb> findByToken(String token);

  int deleteByUser(UserDb user);

  int deleteByToken(String token);
}
