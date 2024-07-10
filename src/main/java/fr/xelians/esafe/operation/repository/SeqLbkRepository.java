/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.operation.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

@Repository
public class SeqLbkRepository {

  @PersistenceContext private EntityManager entityManager;

  public long getNextValue() {
    return (long)
        entityManager.createNativeQuery("select nextval('logbook_seq')").getSingleResult();
  }
}
