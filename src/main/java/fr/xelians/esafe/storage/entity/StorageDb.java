/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class StorageDb {

  @Id @NotNull private Long id;

  @Min(value = -1)
  @NotNull
  private Long secureNumber = -1L;

  public long incSecureNumber() {
    return ++secureNumber;
  }

  // Needed by JPA
  public StorageDb() {}

  public StorageDb(Long tenant) {
    this.id = tenant;
  }
}
