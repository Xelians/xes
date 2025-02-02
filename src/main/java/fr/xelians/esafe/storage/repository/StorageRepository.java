/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.repository;

import fr.xelians.esafe.storage.entity.StorageDb;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * @author Emmanuel Deviller
 */
public interface StorageRepository extends JpaRepository<StorageDb, Long> {}
