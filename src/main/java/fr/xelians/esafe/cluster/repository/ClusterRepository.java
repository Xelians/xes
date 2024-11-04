/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.repository;

import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.entity.JobDb;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Emmanuel Deviller
 */
public interface ClusterRepository extends JpaRepository<JobDb, Long> {

  @Query(value = "SELECT NEXTVAL('job_seq')", nativeQuery = true)
  long getNextValue();

  // INSERT ... DO NOTHING: tells PostgreSQL to create row if it does not exist. This action makes
  // no changes, but suppresses the error (i.e. on conflict do nothing) that would normally occur
  // when inserting a row that violates a condition.
  // cf.
  // https://stackoverflow.com/questions/43665090/why-do-we-have-to-use-modifying-annotation-for-queries-in-data-jpa
  @Transactional
  @Modifying
  @Query(
      value =
          "INSERT INTO job(job_type, expire) VALUES(:#{#jobType.name()}, MAKE_TIMESTAMP(2001, 1, 1, 1, 1, 1)) ON CONFLICT DO NOTHING",
      nativeQuery = true)
  void initJobs(@Param("jobType") JobType jobType);

  @Transactional
  @Query(
      value =
          "UPDATE job SET identifier=:id, expire=LOCALTIMESTAMP WHERE identifier=:id OR LOCALTIMESTAMP - expire > MAKE_INTERVAL(0,0,0,0,0,0,:delay)",
      nativeQuery = true)
  @Modifying
  void refreshJobs(@Param("id") Long id, @Param("delay") double expireDelay);

  @Query(
      value =
          "SELECT identifier FROM job WHERE identifier=:id AND job_type=:#{#jobType.name()} AND LOCALTIMESTAMP - expire < MAKE_INTERVAL(0,0,0,0,0,0,:delay)",
      nativeQuery = true)
  List<Long> findActiveJobs(
      @Param("jobType") JobType jobType, @Param("id") Long id, @Param("delay") double activeDelay);
}
