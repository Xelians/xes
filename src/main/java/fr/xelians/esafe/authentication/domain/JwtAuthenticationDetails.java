/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication.domain;

import jakarta.servlet.http.HttpServletRequest;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
public class JwtAuthenticationDetails extends WebAuthenticationDetails {

  private final Long tenant;
  private final String applicationId;

  public JwtAuthenticationDetails(HttpServletRequest context, Long tenant, String applicationId) {
    super(context);
    this.tenant = tenant;
    this.applicationId = applicationId;
  }
}
