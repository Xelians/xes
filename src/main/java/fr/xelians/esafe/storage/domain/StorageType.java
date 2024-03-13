/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain;

import lombok.Getter;

@Getter
public enum StorageType {
  FS("File System"),
  EFS("Encrypted File System"),
  S3("Amazon S3");

  private final String desc;

  StorageType(String desc) {
    this.desc = desc;
  }
}
