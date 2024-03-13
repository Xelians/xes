/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HashStorageObject extends StorageObjectId {

  private final Hash hash;
  private final byte[] checksum;

  protected HashStorageObject(Long id, StorageObjectType type, Hash hash, byte[] checksum) {
    super(id, type);
    this.hash = hash;
    this.checksum = checksum;
  }

  public static HashStorageObject create(String action) {
    String[] tokens = StringUtils.split(action, ';');
    if (tokens.length >= 4) {
      return HashStorageObject.create(
          Long.parseLong(tokens[0]),
          StorageObjectType.valueOf(tokens[1]),
          Hash.valueOf(tokens[2]),
          HashUtils.decodeHex(tokens[3]));
    }
    throw new InternalException(
        "StorageObject creation failed",
        String.format("Failed to create StorageObject from tokens [%s]", String.join(";", tokens)));
  }

  public static HashStorageObject create(
      long id, StorageObjectType type, Hash hash, byte[] checksum) {
    Validate.notNull(type, "type");
    Validate.notNull(hash, "hash");
    Validate.notNull(checksum, "checksum");

    return new HashStorageObject(id, type, hash, checksum);
  }
}
