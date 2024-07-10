/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.repository;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.entity.ServerNodeDb;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.LockOptions;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ServerNodeRepository extends JpaRepository<ServerNodeDb, Long> {

  // note.
  // https://stackoverflow.com/questions/43665090/why-do-we-have-to-use-modifying-annotation-for-queries-in-data-jpa

  // INSERT ... DO NOTHING: tells PostgreSQL to create row if it does not exist. This action makes
  // no changes, but
  // suppresses the error (i.e. on conflict do nothing) that would normally occur when inserting a
  // row that violates a condition.
  @Transactional
  @Modifying
  @Query(
      value =
          "insert into server_node(feature, delay) values(:#{#feature.name()}, now()) on conflict do nothing",
      nativeQuery = true)
  void upsert(@Param("feature") NodeFeature feature);

  @Query(value = "select nextval('server_node_seq')", nativeQuery = true)
  long getNextValue();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(
      @QueryHint(
          name = AvailableSettings.JAKARTA_LOCK_TIMEOUT,
          value = "" + LockOptions.SKIP_LOCKED))
  @Query("select o from ServerNodeDb o where o.identifier=:id or o.delay<:delay")
  List<ServerNodeDb> findByIdentifierOrDelay(
      @Param("id") long id, @Param("delay") LocalDateTime delay);
}
