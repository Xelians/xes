/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.object;

import fr.xelians.esafe.storage.domain.StorageObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang.Validate;

@Getter
@EqualsAndHashCode
@ToString
public class StorageObjectId {

  private final Long id;
  private final StorageObjectType type;

  public StorageObjectId(Long id, StorageObjectType type) {
    Validate.notNull(id);
    Validate.notNull(type);
    this.id = id;
    this.type = type;
  }
}
