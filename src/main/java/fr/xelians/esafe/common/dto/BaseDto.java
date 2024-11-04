/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.dto;

import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.referential.domain.Status;
import java.time.LocalDate;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface BaseDto {

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  LocalDate getCreationDate();

  void setCreationDate(LocalDate creationDate);

  LocalDate getLastUpdate();

  void setLastUpdate(LocalDate lastUpdate);

  LocalDate getActivationDate();

  void setActivationDate(LocalDate activationDate);

  LocalDate getDeactivationDate();

  void setDeactivationDate(LocalDate deactivationDate);

  Status getStatus();

  void setStatus(Status status);

  List<LifeCycle> getLifeCycles();

  void setLifeCycles(List<LifeCycle> lifeCycles);

  Integer getAutoVersion();

  void setAutoVersion(Integer autoVersion);

  Long getOperationId();

  void setOperationId(Long operationId);
}
