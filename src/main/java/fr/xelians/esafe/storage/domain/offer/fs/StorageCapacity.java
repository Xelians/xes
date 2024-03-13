/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import lombok.Getter;

@Getter
public enum StorageCapacity {
  SMALL("Small Capacity"),
  MEDIUM("Medium Capacity"),
  LARGE("Large Capacity");

  private final String desc;

  StorageCapacity(String desc) {
    this.desc = desc;
  }
}
