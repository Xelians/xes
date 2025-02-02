/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.entity.database;

import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.referential.domain.Status;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public interface BaseDb {

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

  byte[] getLfcs();

  void setLfcs(byte[] lifeCycles);

  Integer getAutoVersion();

  void setAutoVersion(Integer autoVersion);

  Long getOperationId();

  void setOperationId(Long operationId);

  default void setLifeCycles(List<LifeCycle> lifeCycles) {
    setLfcs(lifeCycles == null ? null : JsonService.collToBytes(lifeCycles, JsonConfig.DEFAULT));
  }

  default void addLifeCycle(LifeCycle lifeCycle) {
    List<LifeCycle> lifeCycles = getLifeCycles();
    if (lifeCycles == null) {
      lifeCycles = new ArrayList<>();
    }
    lifeCycles.add(lifeCycle);
    setLifeCycles(lifeCycles);
  }

  default List<LifeCycle> getLifeCycles() {
    try {
      byte[] lfcs = getLfcs();
      return lfcs == null ? null : JsonService.toLifeCycles(lfcs);
    } catch (IOException ex) {
      throw new InternalException("Failed to convert bytes to LifeCycles", ex);
    }
  }

  default int incAutoVersion() {
    int av = getAutoVersion() + 1;
    setAutoVersion(av);
    return av;
  }
}
