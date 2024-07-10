/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.repository;

import fr.xelians.esafe.operation.entity.TaskLockDb;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface TaskLockRepository extends JpaRepository<TaskLockDb, Long> {

  // PESSIMISTIC_WRITE allows us to obtain an exclusive lock and prevent the data from being read,
  // updated or deleted.
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @QueryHints(@QueryHint(name = AvailableSettings.JAKARTA_LOCK_TIMEOUT, value = "10000"))
  @Query("SELECT t FROM TaskLockDb t WHERE t.id=:id")
  Optional<TaskLockDb> findForUpdate(@Param("id") Long id);

  // PESSIMISTIC_READ allows us to obtain a shared lock and prevent the data from being updated or
  // deleted.
  @Lock(LockModeType.PESSIMISTIC_READ)
  @QueryHints(@QueryHint(name = AvailableSettings.JAKARTA_LOCK_TIMEOUT, value = "10000"))
  @Query("SELECT t FROM TaskLockDb t WHERE t.id=:id")
  Optional<TaskLockDb> findForRead(@Param("id") Long id);
}
