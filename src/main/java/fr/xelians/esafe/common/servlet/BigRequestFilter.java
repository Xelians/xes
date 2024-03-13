/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.servlet;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

// This filter permits to prevent big requests except multipart ones
@Slf4j
public class BigRequestFilter extends OncePerRequestFilter {

  // The default max length for all requests (except multipart) is 256KB
  public static final int MAX_LENGTH = 256_000;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String type = request.getContentType();
    if (type == null || !type.startsWith(MULTIPART_FORM_DATA_VALUE)) {
      if (request.getContentLength() > MAX_LENGTH) {
        writeError(request, response);
      } else {
        // Wrap the request with the limited input stream servlet request
        filterChain.doFilter(new LimitedHttpServletRequest(request, MAX_LENGTH), response);
      }
    } else {
      filterChain.doFilter(request, response);
    }
  }

  private void writeError(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    String msg =
        String.format(
            "The request content length is '%s' greater than the max allowed value",
            request.getContentLength());

    String url = URLEncoder.encode(request.getServletPath(), StandardCharsets.UTF_8);

    PbDetail pbDetail =
        PbDetail.builder()
            .status(BAD_REQUEST)
            .title("Request too long")
            .message(msg)
            .code(ExceptionsUtils.createCode())
            .timestamp(Instant.now())
            .tenant(request.getHeader(Header.X_TENANT_ID))
            .instance(URI.create(url))
            .build();

    String uri = request.getMethod() + " - " + request.getServletPath();
    log.warn(ExceptionsUtils.format(pbDetail, msg, uri));

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    ExceptionsUtils.MAPPER.writeValue(response.getOutputStream(), pbDetail);
    response.getOutputStream().flush();
  }
}
