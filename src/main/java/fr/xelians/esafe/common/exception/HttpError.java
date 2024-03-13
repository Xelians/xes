/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.exception;

import java.util.List;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
public class HttpError {

  private final long timestamp;
  private final HttpStatus status;
  private final String name;
  private final String message;
  private final List<String> errors;

  public HttpError(HttpStatus status, String message, String name, String error) {
    this(status, message, name, List.of(error));
  }

  public HttpError(HttpStatus status, String message, String name, List<String> errors) {
    this.timestamp = System.currentTimeMillis();
    this.status = status;
    this.name = name;
    this.message = message;
    this.errors = errors;
  }
}
