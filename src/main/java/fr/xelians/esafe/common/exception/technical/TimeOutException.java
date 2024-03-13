/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception.technical;

import java.net.URI;
import org.springframework.http.HttpStatus;

public class TimeOutException extends TechnicalException {

  public TimeOutException(String title, String message) {
    super(title, message, URI.create(ABOUT_BLANK));
  }

  @Override
  public HttpStatus getHttpStatus() {
    return HttpStatus.GATEWAY_TIMEOUT;
  }
}
