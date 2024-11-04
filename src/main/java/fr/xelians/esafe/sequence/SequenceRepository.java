/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.sequence;

import fr.xelians.esafe.operation.entity.OperationDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/*
 * @author Emmanuel Deviller
 */
public interface SequenceRepository extends JpaRepository<OperationDb, Long> {

  @Query(value = "SELECT currval('global_seq')", nativeQuery = true)
  long getCurrentValue();

  @Query(value = "SELECT nextval('global_seq')", nativeQuery = true)
  long getNextValue();
}
