/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StorageObject extends StorageObjectId {

  private final boolean ignoreChecksum;
  private final boolean ignoreEncryption;

  public StorageObject(
      Long id, StorageObjectType type, boolean ignoreChecksum, boolean ignoreEncryption) {
    super(id, type);
    this.ignoreChecksum = ignoreChecksum;
    this.ignoreEncryption = ignoreEncryption;
  }
}
