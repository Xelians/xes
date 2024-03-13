/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit;

import fr.xelians.esafe.common.utils.SipUtils;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

@Getter
public abstract class RelationRef<T> {

  protected final T reference;

  protected RelationRef(T reference) {
    Validate.notNull(reference, SipUtils.NOT_NULL, "reference");
    this.reference = reference;
  }
}
