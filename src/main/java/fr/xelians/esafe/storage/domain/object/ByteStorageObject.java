/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.util.Assert;

@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ByteStorageObject extends StorageObject {

  private final byte[] bytes;

  public ByteStorageObject(byte[] bytes, Long id, StorageObjectType type) {
    this(bytes, id, type, false, false);
  }

  public ByteStorageObject(byte[] bytes, Long id, StorageObjectType type, boolean ignoreChecksum) {
    this(bytes, id, type, ignoreChecksum, false);
  }

  public ByteStorageObject(
      byte[] bytes,
      Long id,
      StorageObjectType type,
      boolean ignoreChecksum,
      boolean ignoreEncryption) {
    super(id, type, ignoreChecksum, ignoreEncryption);
    Assert.notNull(bytes, "Bytes cannot be null");
    this.bytes = bytes;
  }
}
