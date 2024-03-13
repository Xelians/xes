/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@AllArgsConstructor
public class LoggingFilter extends CommonsRequestLoggingFilter {

  private final String[] pathsToIgnore;

  @Override
  protected boolean shouldLog(HttpServletRequest request) {
    return !StringUtils.startsWithAny(request.getRequestURI(), pathsToIgnore);
  }
}
