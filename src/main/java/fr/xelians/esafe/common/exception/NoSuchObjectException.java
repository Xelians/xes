/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception;

import java.io.IOException;

public class NoSuchObjectException extends IOException {

  public NoSuchObjectException() {
    super();
  }

  public NoSuchObjectException(String message) {
    super(message);
  }

  public NoSuchObjectException(Throwable cause) {
    super(cause);
  }

  public NoSuchObjectException(String message, Throwable cause) {
    super(message, cause);
  }
}
