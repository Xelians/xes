/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ChecksumStorageObject extends StorageObjectId {

  private final Hash hash;
  private final byte[] checksum;

  public ChecksumStorageObject(Long id, StorageObjectType type, Hash hash, byte[] checksum) {
    super(id, type);
    this.hash = hash;
    this.checksum = checksum;
  }
}
