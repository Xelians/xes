/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception.functional;

import java.net.URI;
import org.springframework.http.HttpStatus;

public class NotFoundException extends FunctionalException {

  public NotFoundException(String title, String message) {
    super(title, message, URI.create(ABOUT_BLANK));
  }

  public NotFoundException(String title, String message, URI type) {
    super(title, message, type);
  }

  public NotFoundException(String title, String message, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }
}
