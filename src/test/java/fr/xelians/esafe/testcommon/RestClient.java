/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_ACCESS_CONTRACT_ID;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.ingest.ContextId;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.BinaryVersion;
import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.authentication.dto.RefreshDto;
import fr.xelians.esafe.common.constant.Header;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.exception.technical.TimeOutException;
import fr.xelians.esafe.common.utils.LoggingRequestInterceptor;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.common.utils.UnitUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationResult;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.dto.vitam.VitamExternalEventDto;
import fr.xelians.esafe.organization.dto.*;
import fr.xelians.esafe.referential.dto.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public final class RestClient {

  public static final OperationStatus[] OP_FINAL = {
    OperationStatus.RETRY_INDEX,
    OperationStatus.ERROR_CHECK,
    OperationStatus.ERROR_COMMIT,
    OperationStatus.OK,
    OperationStatus.FATAL
  };

  private static final MediaType TEXT_PLAIN_UTF8 = MediaType.valueOf("text/plain; charset=UTF-8");

  private static final int RETRY = 10;

  // Warning: the client error codes may differ when debug is on
  @Getter @Setter private boolean debug = false;

  private final String server;
  private final boolean autoRefresh;
  private String accessToken;

  private String apiKey;
  private String refreshToken;

  private boolean useApiKey;

  private final RestTemplate restTemplate = createRestTemplate();

  public RestClient(int port) {
    this(port, true);
  }

  public RestClient(int port, boolean autoRefresh) {
    this.server = "http://localhost:" + port;
    this.autoRefresh = autoRefresh;
  }

  private RestTemplate createDebugRestTemplate() {
    RestTemplate rt =
        new RestTemplate(
            new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    rt.setRequestFactory(createJdkHttpFactory());

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
    interceptors.add(new LoggingRequestInterceptor());
    rt.setInterceptors(interceptors);
    return rt;
  }

  public RestTemplate getRestTemplate() {
    return debug ? createDebugRestTemplate() : restTemplate;
  }

  private RestTemplate createRestTemplate() {
    final var restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(createJdkHttpFactory());
    // Configure UTF-8 in content type instead of the following
    //    List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
    //    converters.removeIf(
    //        httpMessageConverter -> httpMessageConverter instanceof StringHttpMessageConverter);
    //    converters.addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    return restTemplate;
  }

  private JdkClientHttpRequestFactory createJdkHttpFactory() {
    final var requestFactory = new JdkClientHttpRequestFactory();
    requestFactory.setReadTimeout(30_000);
    return requestFactory;
  }

  private HttpClient httpClient() {
    return new HttpClient(this);
  }

  // HTTP Header
  public static HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(Header.X_APPLICATION_ID, "ESAFE_TEST");
    return headers;
  }

  public static HttpHeaders createHeaders(String accessToken) {
    HttpHeaders headers = createHeaders();
    if (accessToken != null) {
      headers.set(Header.AUTHORIZATION, "Bearer " + accessToken);
    }
    return headers;
  }

  public static HttpHeaders createHeaders(String accessToken, Long tenant) {
    HttpHeaders headers = createHeaders(accessToken);
    if (tenant != null) {
      headers.set(Header.X_TENANT_ID, String.valueOf(tenant));
    }
    return headers;
  }

  // Batch Report
  public void downloadReport(long tenant, String operationId, Path path) {
    String url = server + ADMIN_EXTERNAL + V1 + BATCH_REPORT;
    httpClient().get(url).tenant(tenant).operationId(operationId).download(path);
  }

  // ATR
  public void downloadXmlAtr(long tenant, String operationId, Path path) {
    String url = server + INGEST_EXTERNAL + V1 + ATR_XML;
    httpClient().get(url).tenant(tenant).operationId(operationId).download(path);
  }

  public void downloadJsonAtr(long tenant, String operationId, Path path) {
    String url = server + INGEST_EXTERNAL + V1 + ATR_JSON;
    httpClient().get(url).tenant(tenant).operationId(operationId).download(path);
  }

  // Manifest
  public void downloadManifest(long tenant, String operationId, Path path) {
    String url = server + INGEST_EXTERNAL + V1 + MANIFESTS;
    httpClient().get(url).tenant(tenant).operationId(operationId).download(path);
  }

  // Ingest
  public ResponseEntity<Void> uploadHolding(long tenant, Path path) {
    return upload(tenant, path, ContextId.HOLDING_SCHEME);
  }

  public ResponseEntity<Void> uploadFiling(long tenant, Path path) {
    return upload(tenant, path, ContextId.FILING_SCHEME);
  }

  public ResponseEntity<Void> uploadSip(long tenant, Path path) throws IOException {
    return upload(tenant, path, ContextId.DEFAULT_WORKFLOW);
  }

  public ResponseEntity<Void> upload(long tenant, Path sipPath, @NotNull ContextId context) {
    String url = server + INGEST_EXTERNAL + V2 + INGESTS;

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(Header.X_FILE, new FileSystemResource(sipPath));

    return httpClient()
        .post(url)
        .tenant(tenant)
        .context(context.toString())
        .body(body)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .execute(Void.class);
  }

  public ResponseEntity<String> updateSip(
      long tenant, String accessContract, String systemId, JsonNode jsonPatch) {
    String url = server + INGEST_EXTERNAL + V1 + INGESTS + "/{systemId}";
    return httpClient()
        .put(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(jsonPatch)
        .param("systemId", systemId)
        .execute(String.class);
  }

  // Admin operations
  public ResponseEntity<Object> updateIndex(long tenant) {
    String url = server + ADMIN_EXTERNAL + V1 + UPDATE_SEARCH_ENGINE_INDEX;
    return httpClient().put(url).tenant(tenant).execute(Object.class);
  }

  public ResponseEntity<?> rebuildIndex(long tenant) {
    String url = server + ADMIN_EXTERNAL + V1 + REBUILD_SEARCH_ENGINE_INDEX;
    return httpClient().post(url).tenant(tenant).execute(Object.class);
  }

  public ResponseEntity<Object> newIndex(long tenant) {
    String url = server + ADMIN_EXTERNAL + V1 + RESET_SEARCH_ENGINE_INDEX;
    return httpClient().post(url).tenant(tenant).execute(Object.class);
  }

  // Admin
  public ResponseEntity<Object> checkCoherency(long tenant, int delay, int duration) {
    String url = server + ADMIN_EXTERNAL + V1 + CHECK_COHERENCE;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .param("delay", delay)
        .param("duration", duration)
        .execute(Object.class);
  }

  // Archive Unit
  public ResponseEntity<JsonNode> getArchiveUnit(
      long tenant, String accessContract, String unitId) {
    return getArchiveUnit(tenant, accessContract, Long.parseLong(unitId));
  }

  public ResponseEntity<JsonNode> getArchiveUnit(long tenant, String accessContract, long unitId) {
    String url = server + ACCESS_EXTERNAL + V2 + UNITS + "/{unitId}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .param("unitId", unitId)
        .execute(JsonNode.class);
  }

  // Update metadata
  public ResponseEntity<String> updateArchive(long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  // Update rules
  public ResponseEntity<String> updateRulesArchive(
      long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS_RULES;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  public ResponseEntity<String> reclassifyArchive(
      long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + RECLASSIFICATION;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  // Elimination
  public ResponseEntity<String> eliminateArchive(long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + ELIMINATION_ACTION;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  // Export
  public ResponseEntity<String> exportArchive(long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + EXPORT;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  public Path downloadDip(long tenant, String operationId, String accessContract, Path tmpDir) {
    Path dipPath = tmpDir.resolve(operationId + ".dip");
    String url = server + ACCESS_EXTERNAL + V1 + EXPORT + "/" + operationId + "/dip";

    for (int i = 0; ; i++) {
      String accessToken = getAccessToken();
      HttpHeaders headers = createHeaders(accessToken, tenant);
      headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
      if (accessContract != null) {
        headers.set(X_ACCESS_CONTRACT_ID, accessContract);
      }

      try {
        getRestTemplate()
            .execute(
                url,
                HttpMethod.GET,
                request -> request.getHeaders().addAll(headers),
                response -> Files.copy(response.getBody(), dipPath, REPLACE_EXISTING));
        return dipPath;
      } catch (HttpClientErrorException e) {
        autoRefresh(e, accessToken, i);
      }
    }
  }

  // Probative value
  public ResponseEntity<String> probativeValue(long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + PROBATIVE_VALUE_EXPORT;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  // Search for archive units
  public ResponseEntity<SearchResult<JsonNode>> searchArchive(
      long tenant, String accessContract, String query) {
    return doSearchArchive(new ParameterizedTypeReference<>() {}, tenant, accessContract, query);
  }

  public ResponseEntity<SearchResult<JsonNode>> searchArchiveWithInheritedRules(
      long tenant, String accessContract, String query) {

    String url = server + ACCESS_EXTERNAL + V1 + UNITS_WITH_INHERITED_RULES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  private <T> ResponseEntity<SearchResult<T>> doSearchArchive(
      ParameterizedTypeReference<SearchResult<T>> responseType,
      long tenant,
      String accessContract,
      String query) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(responseType);
  }

  // List is not the best structure to download a large number of documents. But it should be ok for
  // testing
  public ResponseEntity<List<JsonNode>> searchArchiveStream(
      long tenant, String accessContract, String query) {
    return doSearchArchiveStream(
        new ParameterizedTypeReference<>() {}, tenant, accessContract, query);
  }

  private <T> ResponseEntity<List<T>> doSearchArchiveStream(
      ParameterizedTypeReference<List<T>> responseType,
      long tenant,
      String accessContract,
      String query) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS_STREAM;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(responseType);
  }

  public ResponseEntity<String> searchArchive2(long tenant, String accessContract, String query) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(String.class);
  }

  // Metadata Objects
  public ResponseEntity<SearchResult<JsonNode>> getObjectMetadataByUnit(
      long tenant, String accessContract, long unitId) {
    String url = server + ACCESS_EXTERNAL + V1 + UNITS + "/{unitId}/objects";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .param("unitId", unitId)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<SearchResult<JsonNode>> searchObjectMetadata(
      long tenant, String accessContract, String query) {

    String url = server + ACCESS_EXTERNAL + V1 + OBJECTS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Binary Objects
  public Path getBinaryObjectByUnitId(
      long tenant, Path tmpDir, String accessContract, String unitId, String binaryVersion) {
    BinaryVersion bv = UnitUtils.getBinaryVersion(binaryVersion);
    return getBinaryObjectByUnit(
        tenant, tmpDir, accessContract, Long.parseLong(unitId), bv.qualifier(), bv.version());
  }

  public Path getBinaryObjectByUnit(
      long tenant,
      Path tmpDir,
      String accessContract,
      long unitId,
      BinaryQualifier qualifier,
      Integer version) {

    Path binPath = tmpDir.resolve(unitId + ".bin");
    String url = server + ACCESS_EXTERNAL + V1 + UNITS + "/{unitId}/objects";

    for (int i = 0; ; i++) {
      String accessToken = getAccessToken();
      HttpHeaders headers = createHeaders(accessToken, tenant);
      //      headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      headers.set(Header.X_ACCESS_CONTRACT_ID, accessContract);
      headers.set(Header.X_QUALIFIER, qualifier.toString());
      if (version != null) headers.set(Header.X_VERSION, version.toString());

      try {
        getRestTemplate()
            .execute(
                url,
                HttpMethod.GET,
                request -> request.getHeaders().addAll(headers),
                response -> Files.copy(response.getBody(), binPath, REPLACE_EXISTING),
                unitId);
        return binPath;
      } catch (HttpClientErrorException e) {
        autoRefresh(e, accessToken, i);
      }
    }
  }

  public Path getBinaryObjectById(
      long tenant, Path tmpDir, String accessContract, String binaryId) {
    return getBinaryObjectById(tenant, tmpDir, accessContract, Long.parseLong(binaryId));
  }

  public Path getBinaryObjectById(long tenant, Path tmpDir, String accessContract, long binaryId) {

    Path binPath = tmpDir.resolve(binaryId + ".bin");
    String url = server + ACCESS_EXTERNAL + V1 + OBJECTS + "/{binaryId}";

    for (int i = 0; ; i++) {
      String accessToken = getAccessToken();
      HttpHeaders headers = createHeaders(accessToken, tenant);
      headers.setAccept(List.of(MediaType.APPLICATION_OCTET_STREAM));
      headers.set(Header.X_ACCESS_CONTRACT_ID, accessContract);
      try {
        getRestTemplate()
            .execute(
                url,
                HttpMethod.GET,
                request -> request.getHeaders().addAll(headers),
                response -> Files.copy(response.getBody(), binPath, REPLACE_EXISTING),
                binaryId);
        return binPath;
      } catch (HttpClientErrorException e) {
        autoRefresh(e, accessToken, i);
      }
    }
  }

  // Accession register
  public ResponseEntity<SearchResult<JsonNode>> searchAccessionRegisterDetails(
      long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESSION_REGISTER_DETAILS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<SearchResult<JsonNode>> searchAccessionRegisterSummary(
      long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESSION_REGISTER_SUMMARY;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Vitam Logbook Operation
  public ResponseEntity<VitamLogbookOperationDto> getVitamLogbookOperation(
      long tenant, String operationId) {
    return getVitamLogbookOperation(tenant, Long.parseLong(operationId));
  }

  public ResponseEntity<VitamLogbookOperationDto> getVitamLogbookOperation(
      long tenant, long operationId) {
    String url = server + LOGBOOK_EXTERNAL + V1 + LOGBOOK_OPERATIONS + "/{operationId}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("operationId", operationId)
        .execute(VitamLogbookOperationDto.class);
  }

  public ResponseEntity<SearchResult<JsonNode>> searchVitamLogbookOperation(
      long tenant, String query) {
    return searchVitamLogbook(new ParameterizedTypeReference<>() {}, tenant, query);
  }

  private <T> ResponseEntity<SearchResult<T>> searchVitamLogbook(
      ParameterizedTypeReference<SearchResult<T>> responseType, long tenant, String query) {
    String url = server + LOGBOOK_EXTERNAL + V1 + LOGBOOK_OPERATIONS_SEARCH;
    return httpClient().get(url).tenant(tenant).body(query).execute(responseType);
  }

  public ResponseEntity<JsonNode> getLogbookUnitLifecycles(
      long tenant, String accessContract, long unitId) {
    String url = server + LOGBOOK_EXTERNAL + V1 + LOGBOOK_UNIT_LIFECYCLES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .param("unitId", unitId)
        .execute(JsonNode.class);
  }

  public ResponseEntity<JsonNode> getLogbookObjectLifecycles(
      long tenant, String accessContract, long unitId) {
    String url = server + LOGBOOK_EXTERNAL + V1 + LOGBOOK_OBJECT_LIFECYCLES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .accessContract(accessContract)
        .param("unitId", unitId)
        .execute(JsonNode.class);
  }

  // Standard Logbook Operation
  public ResponseEntity<LogbookOperationDto> getLogbookOperation(long tenant, String operationId) {
    String url = server + LOGBOOK_EXTERNAL + V2 + LOGBOOK_OPERATIONS + "/{operationId}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("operationId", operationId)
        .execute(LogbookOperationDto.class);
  }

  public ResponseEntity<SearchResult<LogbookOperationDto>> searchLogbookOperation(
      long tenant, String query) {
    String url = server + LOGBOOK_EXTERNAL + V2 + LOGBOOK_OPERATIONS_SEARCH;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Vitam Operation
  public ResponseEntity<JsonNode> getVitamOperation(long tenant, String operationId) {
    String url = server + ADMIN_EXTERNAL + V1 + OPERATIONS_ID;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("operationId", operationId)
        .execute(JsonNode.class);
  }

  public ResponseEntity<JsonNode> searchVitamOperations(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + OPERATIONS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Operation
  public ResponseEntity<OperationDto> getOperation(long tenant, String operationId) {
    String url = server + ADMIN_EXTERNAL + V2 + OPERATIONS_ID;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("operationId", operationId)
        .execute(OperationDto.class);
  }

  public ResponseEntity<OperationResult<OperationDto>> searchOperations(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V2 + OPERATIONS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Operation status
  public ResponseEntity<OperationStatusDto> getOperationStatus(long tenant, String operationId) {
    String url = server + ADMIN_EXTERNAL + V1 + OPERATIONS_ID_STATUS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("operationId", operationId)
        .execute(OperationStatusDto.class);
  }

  public ResponseEntity<OperationResult<OperationStatusDto>> searchOperationsStatus(
      long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + OPERATIONS_STATUS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public OperationStatusDto waitForOperationStatus(
      long tenant, String operationId, int sec, OperationStatus... expectedStatus) {
    for (int j = 0; j < sec * 5; j++) {
      ResponseEntity<OperationStatusDto> response = getOperationStatus(tenant, operationId);
      OperationStatusDto operationStatus = response.getBody();

      if (operationStatus == null) {
        throw new InternalException(
            "Failed to get operation", String.format("Operation is null - id: '%s'", operationId));
      }
      if (Arrays.stream(expectedStatus).anyMatch(status -> operationStatus.status() == status)) {
        return operationStatus;
      }
      Utils.sleep(200); // Avoid VM exit - update with Operation
    }

    throw new TimeOutException(
        "Operation time out",
        String.format("Failed to process operationId '%s' - tenant '%s'", operationId, tenant));
  }

  public List<OperationStatusDto> waitForOperationsStatus(
      long tenant, List<String> operationIds, int sec, OperationStatus... expectedStatus) {

    Set<String> operationSet = new HashSet<>(operationIds);
    List<OperationStatusDto> operationStatusDtos = new ArrayList<>(operationIds.size());
    long period = 1000;

    OUTER:
    for (Iterator<String> i = operationSet.iterator(); i.hasNext(); ) {
      String operationId = i.next();
      for (int j = 0; j < sec * (int) (1000 / period); j++) {
        ResponseEntity<OperationStatusDto> response = getOperationStatus(tenant, operationId);
        OperationStatusDto operationDto = response.getBody();
        if (operationDto == null) {
          throw new InternalException(
              "Failed to get operation",
              String.format("Operation is null - id: '%s'", operationId));
        }
        if (Arrays.stream(expectedStatus).anyMatch(status -> operationDto.status() == status)) {
          i.remove();
          operationStatusDtos.add(operationDto);
          if (operationSet.isEmpty()) {
            return operationStatusDtos;
          }
          continue OUTER;
        }
        Utils.sleep(period); // Avoid VM exit - update with Operation
      }
      throw new TimeOutException(
          "Operation time out",
          String.format("Failed to process operationId '%s' - tenant '%s'", operationId, tenant));
    }

    throw new InternalException("Operation Failed", "Wait for operationId");
  }

  public OperationDto waitForOperation(
      long tenant, String operationId, int sec, OperationStatus... expectedStatus) {
    for (int j = 0; j < sec * 10; j++) {
      ResponseEntity<OperationDto> response = getOperation(tenant, operationId);
      OperationDto operation = response.getBody();
      if (operation == null) {
        throw new InternalException(
            "Failed to get operation", String.format("Operation is null - id: '%s'", operationId));
      }
      if (Arrays.stream(expectedStatus).anyMatch(status -> operation.status() == status)) {
        return operation;
      }
      Utils.sleep(100); // Avoid VM exit - update with Operation
    }

    throw new TimeOutException(
        "Operation time out",
        String.format("Failed to process operationId '%s' - tenant '%s'", operationId, tenant));
  }

  // External operation
  public ResponseEntity<JsonNode> createExternalOperation(
      long tenant, VitamExternalEventDto vitamExternalEventDto) throws IOException {

    String url = server + ADMIN_EXTERNAL + V1 + LOGBOOK_OPERATIONS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(vitamExternalEventDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Rule V1
  public ResponseEntity<List<RuleDto>> createCsvRule(long tenant, Path csvPath) throws IOException {
    String csv = Files.readString(csvPath, StandardCharsets.UTF_8);
    String url = server + ADMIN_EXTERNAL + V1 + RULES;
    return httpClient()
        .contentType(TEXT_PLAIN_UTF8)
        .post(url)
        .tenant(tenant)
        .body(csv)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<String> getCsvRules(long tenant) {
    String url = server + ADMIN_EXTERNAL + V1 + RULES + "/csv";
    return httpClient().get(url).tenant(tenant).contentType(TEXT_PLAIN_UTF8).execute(String.class);
  }

  public ResponseEntity<RuleDto> getRuleByIdentifier(long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + RULES + "/{identifier}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<SearchResult<JsonNode>> searchRules(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + RULES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Rule V2
  public ResponseEntity<List<RuleDto>> createRule(long tenant, RuleDto... ruleDto) {
    String url = server + ADMIN_EXTERNAL + V2 + RULES;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(ruleDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<PageResult<RuleDto>> getRules(long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + RULES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Profile
  public ResponseEntity<List<ProfileDto>> createProfile(long tenant, Path profilePath)
      throws IOException {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES;
    byte[] bytes = Files.readAllBytes(profilePath);
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(bytes)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<List<ProfileDto>> createProfile(long tenant, ProfileDto... profileDto) {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(profileDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<ProfileDto> updateProfile(
      long tenant, ProfileDto profileDto, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES + "/{identifier}";
    return httpClient()
        .put(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .body(profileDto)
        .execute(ProfileDto.class);
  }

  public ResponseEntity<Void> updateBinaryProfile(long tenant, Path profilePath, String identifier)
      throws IOException {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES + "/" + identifier + "/data";

    MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
    multipartBodyBuilder.part(
        "file", new FileSystemResource(profilePath), MediaType.APPLICATION_OCTET_STREAM);

    return httpClient()
        .put(url)
        .tenant(tenant)
        .body(multipartBodyBuilder.build())
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .execute(Void.class);
  }

  public ResponseEntity<PageResult<ProfileDto>> getProfiles(
      long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + PROFILES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<ProfileDto> getProfileByIdentifier(long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES + "/{identifier}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<byte[]> getBinaryProfile(long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + PROFILES + "/" + identifier + "/data";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .execute(byte[].class);
  }

  public ResponseEntity<SearchResult<JsonNode>> searchProfiles(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V2 + PROFILES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Access Contract V1
  public ResponseEntity<List<AccessContractDto>> createAccessContract(long tenant, Path acPath)
      throws IOException {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESS_CONTRACTS;
    byte[] bytes = Files.readAllBytes(acPath);
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(bytes)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<List<AccessContractDto>> createAccessContract(
      long tenant, AccessContractDto... accessContractDto) {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESS_CONTRACTS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(accessContractDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<AccessContractDto> getAccessContractByIdentifier(
      long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESS_CONTRACTS + "/{identifier}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<SearchResult<JsonNode>> searchAccessContracts(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + ACCESS_CONTRACTS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Access Contract V2
  public ResponseEntity<PageResult<AccessContractDto>> getAccessContracts(
      long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + ACCESS_CONTRACTS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Ingest Contract V1
  public ResponseEntity<List<IngestContractDto>> createIngestContract(long tenant, Path icPath)
      throws IOException {
    String url = server + ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS;
    byte[] bytes = Files.readAllBytes(icPath);
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(bytes)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<List<IngestContractDto>> createIngestContract(
      long tenant, IngestContractDto... ingestContractDto) {
    String url = server + ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(ingestContractDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<IngestContractDto> getIngestContractByIdentifier(
      long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS + "/{identifier}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .execute(IngestContractDto.class);
  }

  public ResponseEntity<SearchResult<JsonNode>> searchIngestContracts(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<IngestContractDto> updateIngestContract(
      long tenant, IngestContractDto ingestContractDto) {
    String url = server + ADMIN_EXTERNAL + V1 + INGEST_CONTRACTS + "/{identifier}";
    return httpClient()
        .put(url)
        .tenant(tenant)
        .body(ingestContractDto)
        .param("identifier", ingestContractDto.getIdentifier())
        .execute(IngestContractDto.class);
  }

  // Ingest Contract V2
  public ResponseEntity<PageResult<IngestContractDto>> getIngestContracts(
      long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + INGEST_CONTRACTS;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Ontologies
  public ResponseEntity<List<OntologyDto>> createOntologies(long tenant, Path indexMapPath)
      throws IOException {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES;
    byte[] bytes = Files.readAllBytes(indexMapPath); // Expects UTF-8 encoding
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(bytes)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<List<OntologyDto>> createOntologies(
      long tenant, OntologyDto... ontologyDtos) {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(ontologyDtos)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<SearchResult<JsonNode>> searchOntologies(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<PageResult<OntologyDto>> getOntologies(long tenant) {
    String url = server + ADMIN_EXTERNAL + V2 + ONTOLOGIES;
    return httpClient().get(url).tenant(tenant).execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<PageResult<OntologyDto>> getOntologies(
      long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + ONTOLOGIES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<OntologyDto> getOntologyByIdentifier(long tenant, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES + "/{identifier}";
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("identifier", identifier)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<PageResult<OntologyDto>> getOntologyByName(long tenant, String name) {
    String url = server + ADMIN_EXTERNAL + V2 + ONTOLOGIES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .param("name", name)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<OntologyDto> updateOntology(
      long tenant, OntologyDto ontologyDto, String identifier) {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES + "/" + identifier;
    return httpClient().put(url).tenant(tenant).body(ontologyDto).execute(OntologyDto.class);
  }

  public ResponseEntity<OntologyDto> updateOntology(long tenant, OntologyDto ontologyDto) {
    String url = server + ADMIN_EXTERNAL + V1 + ONTOLOGIES + "/{identifier}";
    return httpClient()
        .put(url)
        .tenant(tenant)
        .body(ontologyDto)
        .param("identifier", ontologyDto.getIdentifier())
        .execute(OntologyDto.class);
  }

  // Agency V1
  public ResponseEntity<List<AgencyDto>> createAgencies(long tenant, Path csvPath)
      throws IOException {
    String csv = Files.readString(csvPath, StandardCharsets.UTF_8);
    String url = server + ADMIN_EXTERNAL + V1 + AGENCIES;
    return httpClient()
        .contentType(TEXT_PLAIN_UTF8)
        .post(url)
        .tenant(tenant)
        .body(csv)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<String> getCsvAgencies(long tenant) {
    String url = server + ADMIN_EXTERNAL + V1 + AGENCIES + "/csv";
    return httpClient().get(url).tenant(tenant).execute(String.class);
  }

  public ResponseEntity<SearchResult<JsonNode>> searchAgencies(long tenant, String query) {
    String url = server + ADMIN_EXTERNAL + V1 + AGENCIES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .body(query)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Agency V2
  public ResponseEntity<List<AgencyDto>> createAgencies(long tenant, AgencyDto... agencyDto) {
    String url = server + ADMIN_EXTERNAL + V2 + AGENCIES;
    return httpClient()
        .post(url)
        .tenant(tenant)
        .body(agencyDto)
        .execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<PageResult<AgencyDto>> getAgencies(
      long tenant, Map<String, Object> params) {
    String url = server + ADMIN_EXTERNAL + V2 + AGENCIES;
    return httpClient()
        .get(url)
        .tenant(tenant)
        .params(params)
        .execute(new ParameterizedTypeReference<>() {});
  }

  // Organization
  public ResponseEntity<OrganizationDto> updateOrganization(OrganizationDto organizationDto) {
    String url = server + V1 + ORGANIZATIONS;
    return httpClient().put(url).body(organizationDto).execute(OrganizationDto.class);
  }

  // User Info
  public ResponseEntity<UserInfoDto> getMe() {
    String url = server + V1 + ME;
    return httpClient().get(url).execute(UserInfoDto.class);
  }

  // User
  public ResponseEntity<List<UserDto>> createUsers(UserDto... userDtos) {
    String url = server + V1 + USERS;
    return httpClient().post(url).body(userDtos).execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<UserDto> updateUser(UserDto userDto) {
    String url = server + V1 + USERS + "/{identifier}";
    return httpClient()
        .put(url)
        .body(userDto)
        .param("identifier", userDto.getIdentifier())
        .execute(UserDto.class);
  }

  public ResponseEntity<UserDto> getUser(String identifier) {
    String url = server + V1 + USERS + "/{identifier}";
    return httpClient().get(url).param("identifier", identifier).execute(UserDto.class);
  }

  public ResponseEntity<List<UserDto>> listUsers() {
    String url = server + V1 + USERS;
    return httpClient().get(url).execute(new ParameterizedTypeReference<>() {});
  }

  // Tenant
  public ResponseEntity<TenantDto> getTenant(long tenant) {
    String url = server + V1 + TENANT + "/{tenant}";
    return httpClient().get(url).param("tenant", tenant).execute(TenantDto.class);
  }

  public ResponseEntity<List<TenantDto>> listTenants() {
    String url = server + V1 + TENANTS;
    return httpClient().get(url).execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<List<TenantDto>> createTenants(TenantDto... tenantDtos) {
    String url = server + V1 + TENANTS;
    return httpClient().post(url).body(tenantDtos).execute(new ParameterizedTypeReference<>() {});
  }

  public ResponseEntity<String> addStorageOffer(long tenant, String offer) {
    String url = server + ADMIN_EXTERNAL + V1 + ADD_STORAGE_OFFER;
    return httpClient().put(url).tenant(tenant).param("offer", offer).execute(String.class);
  }

  // Signup
  public ResponseEntity<SignupDto> signup(SignupDto signupDto) {
    signupRegister(signupDto);
    return signupCreate(signupDto.getOrganizationDto().getIdentifier());
  }

  public ResponseEntity<Void> signupRegister(SignupDto signupDto) {
    String url = server + V1 + SIGNUP;
    HttpEntity<SignupDto> entity = new HttpEntity<>(signupDto, createHeaders());
    return getRestTemplate().exchange(url, HttpMethod.POST, entity, Void.class);
  }

  public ResponseEntity<SignupDto> signupCreate(String key) {
    String url = server + V1 + SIGNUP + "/{key}";
    return getRestTemplate()
        .exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders()), SignupDto.class, key);
  }

  public ResponseEntity<AccessDto> signin(LoginDto loginDto) {
    String url = server + V1 + SIGNIN;
    HttpEntity<LoginDto> entity = new HttpEntity<>(loginDto, createHeaders());
    ResponseEntity<AccessDto> response =
        getRestTemplate().exchange(url, HttpMethod.POST, entity, AccessDto.class);
    AccessDto accessDto = response.getBody();
    if (accessDto != null) {
      setAccessToken(accessDto.getAccessToken());
      setRefreshToken(accessDto.getRefreshToken());
      return response;
    }
    throw new InternalException("Failed to signin", "AccessDto is null");
  }

  public ResponseEntity<Object> logout() {
    String url = server + V1 + LOGOUT;
    HttpEntity<Object> entity = new HttpEntity<>(createHeaders(getAccessToken()));
    try {
      return getRestTemplate().exchange(url, HttpMethod.POST, entity, Object.class);
    } finally {
      setApiKey(null);
      setAccessToken(null);
      setRefreshToken(null);
    }
  }

  public void autoRefresh(HttpClientErrorException e, String accessToken, int i) {
    if (!autoRefresh || e.getStatusCode() != HttpStatus.UNAUTHORIZED || i >= RETRY) {
      // if (!autoRefresh || i >= RETRY) {
      throw e;
    }
    doRefresh(new RefreshDto(accessToken, getRefreshToken()));
  }

  private synchronized void doRefresh(RefreshDto refreshDto) {
    if (refreshDto.getAccessToken().equals(getAccessToken())) {
      String url = server + V1 + REFRESH;
      HttpEntity<RefreshDto> entity = new HttpEntity<>(refreshDto, createHeaders());
      ResponseEntity<AccessDto> response =
          getRestTemplate().exchange(url, HttpMethod.POST, entity, AccessDto.class);
      AccessDto accessDto = response.getBody();
      if (accessDto == null) {
        throw new InternalException("Failed to refresh access token", "AccessDto is null");
      }
      setAccessToken(accessDto.getAccessToken());
    }
  }

  public ResponseEntity<AccessDto> refresh(RefreshDto refreshDto) {
    String url = server + V1 + REFRESH;
    HttpEntity<RefreshDto> entity = new HttpEntity<>(refreshDto, createHeaders());
    ResponseEntity<AccessDto> response =
        getRestTemplate().exchange(url, HttpMethod.POST, entity, AccessDto.class);
    AccessDto accessDto = response.getBody();
    if (accessDto == null) {
      throw new InternalException("Failed to refresh access token", "AccessDto is null");
    }
    setAccessToken(accessDto.getAccessToken());
    return response;
  }

  public synchronized String getAccessToken() {
    return accessToken;
  }

  public synchronized void setAccessToken(String token) {
    accessToken = token;
  }

  public synchronized void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public synchronized String getApiKey() {
    return apiKey;
  }

  public synchronized boolean useApiKey() {
    return this.useApiKey;
  }

  public synchronized boolean setUseApiKey(boolean useApiKey) {
    return this.useApiKey = useApiKey;
  }

  public synchronized String getRefreshToken() {
    return refreshToken;
  }

  public synchronized void setRefreshToken(String token) {
    refreshToken = token;
  }
}
