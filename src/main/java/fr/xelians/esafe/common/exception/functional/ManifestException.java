/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception.functional;

import java.net.URI;
import org.springframework.http.HttpStatus;

public class ManifestException extends FunctionalException {

  public ManifestException(String title, String message) {
    super(title, message, URI.create(ABOUT_BLANK));
  }

  public ManifestException(String title, String message, Throwable cause) {
    super(title, message, URI.create(ABOUT_BLANK), cause);
  }

  public ManifestException(String title, String message, int code, URI type) {
    super(title, message, type);
  }

  public ManifestException(String title, String message, int code, URI type, Throwable cause) {
    super(title, message, type, cause);
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.BAD_REQUEST;
  }
}
