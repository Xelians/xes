/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class TaskLockDb {

  @Id @NotNull private Long id;

  @NotNull private boolean exclusiveLock = false;

  // Needed by JPA
  public TaskLockDb() {}

  public TaskLockDb(Long tenant) {
    this.id = tenant;
  }
}
