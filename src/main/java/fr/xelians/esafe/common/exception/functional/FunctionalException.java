/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception.functional;

import fr.xelians.esafe.common.exception.Category;
import fr.xelians.esafe.common.exception.EsafeException;
import java.net.URI;

public abstract class FunctionalException extends EsafeException {

  protected FunctionalException(String message, URI type) {
    super(message, type);
  }

  protected FunctionalException(String title, String message, URI type) {
    super(title, message, type);
  }

  protected FunctionalException(String title, String message, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public Category getCategory() {
    return Category.FUNCTIONAL;
  }
}
