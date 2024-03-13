/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.servlet;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class LimitedHttpServletRequest extends HttpServletRequestWrapper {
  private final long maxLength;

  public LimitedHttpServletRequest(HttpServletRequest request, long maxLength) {
    super(request);
    this.maxLength = maxLength;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return new LimitedServletInputStream(super.getInputStream(), maxLength);
  }
}
