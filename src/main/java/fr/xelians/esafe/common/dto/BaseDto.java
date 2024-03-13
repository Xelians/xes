/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.dto;

import fr.xelians.esafe.archive.domain.unit.LifeCycle;
import fr.xelians.esafe.referential.domain.Status;
import java.time.LocalDate;
import java.util.List;

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

  int getAutoVersion();

  void setAutoVersion(int autoVersion);

  Long getOperationId();

  void setOperationId(Long operationId);
}
