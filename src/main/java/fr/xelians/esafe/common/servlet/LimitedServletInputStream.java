/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.servlet;

import fr.xelians.esafe.common.exception.functional.BadRequestException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class LimitedServletInputStream extends ServletInputStream {

  public static final String REQUEST_TOO_LARGE = "The request size is too large";

  private final ServletInputStream sis;
  private final AtomicLong bread = new AtomicLong();
  private final long maxLength;
  private boolean isFinished = false;

  public LimitedServletInputStream(ServletInputStream sis, long maxLength) {
    this.sis = sis;
    this.maxLength = maxLength;
  }

  @Override
  public int readLine(byte[] b, int off, int len) throws IOException {
    int c = super.readLine(b, off, len);
    if (c != -1 && bread.addAndGet(c) > maxLength) {
      isFinished = true;
      throw new BadRequestException(REQUEST_TOO_LARGE);
    }
    return c;
  }

  @Override
  public boolean isFinished() {
    return isFinished || sis.isFinished();
  }

  @Override
  public boolean isReady() {
    return sis.isReady();
  }

  @Override
  public void setReadListener(ReadListener listener) {
    sis.setReadListener(listener);
  }

  @Override
  public int read() throws IOException {
    int c = sis.read();
    if (c != -1 && bread.incrementAndGet() > maxLength) {
      isFinished = true;
      throw new BadRequestException(REQUEST_TOO_LARGE);
    }
    return c;
  }
}
