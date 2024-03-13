/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception.technical;

import fr.xelians.esafe.common.exception.Category;
import fr.xelians.esafe.common.exception.EsafeException;
import java.net.URI;

public abstract class TechnicalException extends EsafeException {

  protected TechnicalException(String title, String message, URI type) {
    super(title, message, type);
  }

  protected TechnicalException(String title, String message, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public Category getCategory() {
    return Category.TECHNICAL;
  }
}
