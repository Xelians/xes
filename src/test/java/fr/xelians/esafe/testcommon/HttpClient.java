/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.testcommon;

import static fr.xelians.esafe.common.constant.Header.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

public class HttpClient {

  private final RestClient restClient;
  private HttpMethod method;
  private String url;
  private Long tenant;
  private MediaType contentType = MediaType.APPLICATION_JSON;
  private ContentDisposition contentDisposition;
  private String accessContract;
  private String context;
  private String operationId;
  private Object body;
  private Map<String, Object> params = new HashMap<>();

  public HttpClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public HttpClient put(String url) {
    this.method = HttpMethod.PUT;
    this.url = url;
    return this;
  }

  public HttpClient post(String url) {
    this.method = HttpMethod.POST;
    this.url = url;
    return this;
  }

  public HttpClient get(String url) {
    this.method = HttpMethod.GET;
    this.url = url;
    return this;
  }

  public HttpClient tenant(long tenant) {
    this.tenant = tenant;
    return this;
  }

  public HttpClient contentType(MediaType contentType) {
    this.contentType = contentType;
    return this;
  }

  public HttpClient contentDisposition(ContentDisposition contentDisposition) {
    this.contentDisposition = contentDisposition;
    return this;
  }

  public HttpClient accessContract(String accessContract) {
    this.accessContract = accessContract;
    return this;
  }

  public HttpClient operationId(String operationId) {
    this.operationId = operationId;
    return this;
  }

  public HttpClient context(String context) {
    this.context = context;
    return this;
  }

  public HttpClient body(Object body) {
    this.body = body;
    return this;
  }

  public <T> HttpClient param(String key, Object value) {
    this.params.put(key, value);
    return this;
  }

  public HttpClient params(Map<String, Object> params) {
    this.params.putAll(params);
    return this;
  }

  private String createUriTemplate() {
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
    params
        .keySet()
        .forEach(
            key -> {
              String tk = "{" + key + "}";
              if (!url.contains(tk)) builder.queryParam(key, tk);
            });
    return builder.encode().toUriString();
  }

  public void download(Path path) {
    String uri = createUriTemplate();

    for (int i = 0; ; i++) {
      String accessToken = restClient.getAccessToken();
      HttpHeaders headers = RestClient.createHeaders(accessToken, tenant);
      try {
        restClient
            .getRestTemplate()
            .execute(
                uri,
                method,
                request -> request.getHeaders().addAll(headers),
                response -> Files.copy(response.getBody(), path, REPLACE_EXISTING),
                operationId);
        return;
      } catch (HttpClientErrorException e) {
        restClient.autoRefresh(e, accessToken, i);
      }
    }
  }

  public <T> ResponseEntity<T> execute(Class<T> responseType) {
    String uri = createUriTemplate();
    for (int i = 0; ; i++) {
      String accessToken = restClient.getAccessToken();
      try {
        return restClient
            .getRestTemplate()
            .exchange(uri, method, createEntity(accessToken), responseType, params);
      } catch (HttpClientErrorException e) {
        restClient.autoRefresh(e, accessToken, i);
      }
    }
  }

  public <T> ResponseEntity<T> execute(ParameterizedTypeReference<T> responseType) {
    String uri = createUriTemplate();
    for (int i = 0; ; i++) {
      String accessToken = restClient.getAccessToken();
      try {
        return restClient
            .getRestTemplate()
            .exchange(uri, method, createEntity(accessToken), responseType, params);
      } catch (HttpClientErrorException e) {
        restClient.autoRefresh(e, accessToken, i);
      }
    }
  }

  private HttpEntity<?> createEntity(String accessToken) {
    HttpHeaders headers = RestClient.createHeaders(accessToken, tenant);
    if (restClient.useApiKey()) {
      headers.set(X_API_KEY_ID, restClient.getApiKey());
    }
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    if (accessContract != null) {
      headers.set(X_ACCESS_CONTRACT_ID, accessContract);
    }
    if (context != null) {
      headers.set(X_CONTEXT_ID, context);
    }
    if (Objects.nonNull(contentDisposition)) {
      headers.set(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
    }
    return body == null ? new HttpEntity<>(headers) : new HttpEntity<>(body, headers);
  }
}
