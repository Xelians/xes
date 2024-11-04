/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Execution(ExecutionMode.SAME_THREAD)
class ArchiveTransferIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void transferTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[3];
    Arrays.fill(sips, 0, sips.length, sip);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sips);
    List<Long> id2s = ids.stream().map(m -> m.get("UNIT_ID2")).toList();
    List<Long> id3s = ids.stream().map(m -> m.get("UNIT_ID3")).toList();

    String requestId = transferArchives(tenant, id2s, id3s);
    searchExported(tenant);

    Path sipPath = getSipPath(tmpDir, tenant, requestId);
    Path atrPath = ingestAndGetATR(tmpDir, sipPath);
    transferAtr(tenant, atrPath);

    searchNonExported(tenant);
  }

  private @NotNull Path getSipPath(Path tmpDir, Long tenant, String requestId) throws IOException {
    Path sipPath = restClient.downloadSip(tenant, requestId, acIdentifier, tmpDir);
    assertNotNull(sipPath);
    assertTrue(Files.exists(sipPath));
    assertTrue(Files.size(sipPath) > 1000);
    return sipPath;
  }

  private String transferArchives(Long tenant, List<Long> id2s, List<Long> id3s) {
    String transferQuery =
        """
                {
                  "dataObjectVersionToExport": {
                    "dataObjectVersions": [ "BinaryMaster", "Thumbnail"]
                  },
                  "transferWithLogBookLFC": true,
                  "transferRequestParameters": {
                    "archivalAgreement" : "IC-000001",
                    "archivalAgencyIdentifier": "AGENCY-000001",
                    "originatingAgencyIdentifier": "AGENCY-000001",
                    "comment": "Test de transfert"
                  },
                  "dslRequest": {
                    "$roots": [],
                    "$type": "DOCTYPE-000001",
                    "$query": [
                      {
                        "$or": [
                          {
                            "$match": {
                              "Title": "MyTitle3"
                            }
                          },
                          {
                            "$match": {
                              "Title": "MyTitle2"
                            }
                          }
                        ]
                      }
                    ],
                    "$filter": {},
                    "$projection": {"$fields": { "Title": 1, "Description": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                  },
                  "maxSizeThreshold": "1000000",
                  "sedaVersion": "2.2"
                }
                """;

    ResponseEntity<String> r2 = restClient.transferArchive(tenant, acIdentifier, transferQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    for (Long id : id2s) {
      ResponseEntity<JsonNode> r1 = restClient.getArchiveUnit(tenant, acIdentifier, id);
      JsonNode unit = r1.getBody();
      assertNotNull(unit);
      assertTrue(unit.get("#transferred").asBoolean());
    }

    for (Long id : id3s) {
      ResponseEntity<JsonNode> r1 = restClient.getArchiveUnit(tenant, acIdentifier, id);
      JsonNode unit = r1.getBody();
      assertNotNull(unit);
      assertTrue(unit.get("#transferred").asBoolean());
    }
    return requestId;
  }

  private Path ingestAndGetATR(Path tmpDir, Path sipPath) throws IOException {
    Long tenant2 = nextTenant();
    Scenario.createScenario02(restClient, tenant2, userDto);

    ResponseEntity<Void> response = restClient.uploadSip(tenant2, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String req2 = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto op2 =
        restClient.waitForOperationStatus(tenant2, req2, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, op2.status());

    Path atrPath = tmpDir.resolve(req2 + ".atr");
    restClient.downloadXmlAtr(tenant2, req2, atrPath);
    assertNotNull(atrPath);
    assertTrue(Files.exists(atrPath));
    assertTrue(Files.size(atrPath) > 1000);
    return atrPath;
  }

  private void transferAtr(Long tenant, Path atrPath) throws IOException {
    ResponseEntity<String> response =
        restClient.transferReplyArchive(tenant, acIdentifier, atrPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));
    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(response));
  }

  private void searchExported(Long tenant) {
    String query =
        """
               {
                 "$roots": [],
                 "$query": [
                   {
                      "$or": [
                        { "$match": { "Title": "MyTitle2" } },
                        { "$match": { "Title": "MyTitle3" } }
                      ]
                   }
                 ],
                 "$filter": {},
                 "$projection": {}
              }
              """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(6, searchResult.results().size(), TestUtils.getBody(response));

    for (JsonNode result : searchResult.results()) {
      assertTrue(result.get("#transferred").asBoolean());
      assertEquals(2, result.get("#version").asInt());
      assertEquals(1, result.get("#lifecycles").size());
    }
  }

  private void searchNonExported(Long tenant) {
    String query =
        """
           {
             "$roots": [],
             "$query": [
               {
                 "$match_phrase_prefix": { "Title": "MyTitle" }
               }
             ],
             "$filter": {},
             "$projection": {}
          }
          """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(3, searchResult.results().size(), TestUtils.getBody(response));

    for (JsonNode result : searchResult.results()) {
      assertEquals("MyTitle1", result.get("Title").asText());
      assertEquals(1, result.get("#version").asInt());
      assertEquals(0, result.get("#lifecycles").size());
    }
  }
}
