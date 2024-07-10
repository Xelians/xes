/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.common.entity.database.BaseDb;

public interface ReferentialDb extends BaseDb {

  Long getId();

  void setId(Long id);

  String getIdentifier();

  void setIdentifier(String identifier);

  Long getTenant();

  void setTenant(Long tenant);
}
