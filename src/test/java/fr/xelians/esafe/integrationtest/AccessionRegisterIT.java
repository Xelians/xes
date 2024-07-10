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
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import nu.xom.ParsingException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AccessionRegisterIT extends BaseIT {

  private final int identifier = 1;
  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void searchForAccessionDetails(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createComplexSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));

    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    String query =
        """
            {
              "$query" : {},
              "$filter" : {
                "$offset" : 0,
                "$limit" : 20,
                "$orderby" : {
                  "#id" : 1
                }
              },
              "$projection" : {}
            }
            """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterDetails(tenant, query),
                r -> r.getBody() != null && r.getBody().hits().size() == 1);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(1, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(1, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("SubmissionAgency").asText(), TestUtils.getBody(r2));
    assertEquals("STORED_AND_COMPLETED", result.get("Status").asText(), TestUtils.getBody(r2));
    assertEquals("INGEST", result.get("OpType").asText(), TestUtils.getBody(r2));

    JsonNode totalObjectGroups = result.get("TotalObjectGroups");
    assertEquals(3, totalObjectGroups.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjectGroups.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(3, totalObjectGroups.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(3, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(3, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalObjects = result.get("TotalObjects");
    assertEquals(8, totalObjects.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjects.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(8, totalObjects.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode event = result.get("Events").get(0);
    assertEquals("INGEST", event.get("OpType").asText(), TestUtils.getBody(r2));
    assertEquals(3, event.get("Gots").asLong(), TestUtils.getBody(r2));
    assertEquals(3, event.get("Units").asLong(), TestUtils.getBody(r2));
    assertEquals(8, event.get("Objects").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void searchForAccessionDetailsWithProjection(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createComplexSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));

    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    String query =
        """
                {
                  "$query" : {
                    "$and": [
                      {"$gte": { "StartDate": "%s" }},
                      {"$eq": { "Status": "STORED_AND_COMPLETED" }}
                    ]
                  },
                  "$filter" : {
                    "$offset" : 0,
                    "$limit" : 20,
                    "$orderby" : {
                      "#id" : 1
                    }
                  },
                  "$projection": {"$fields": { "#tenant": 1 , "#version": 1 , "LegalStatus": 1, "TotalUnits.deleted": 1, "Events": 1, "StartDate":1 }}
                }
                """
            .formatted(LocalDateTime.now().minusYears(10));

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterDetails(tenant, query),
                r -> r.getBody() != null && r.getBody().hits().size() == 1);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(1, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(tenant, result.get("#tenant").asLong(), TestUtils.getBody(r2));
    assertEquals(1, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("Public Archive", result.get("LegalStatus").asText(), TestUtils.getBody(r2));
    assertNull(result.get("OriginatingAgency"), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(0, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));

    JsonNode event = result.get("Events").get(0);
    assertEquals(3, event.get("Gots").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void searchForAccessionDetailsAfterElimination(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createComplexSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    // Eliminate
    String acIdentifier = "AC-" + TestUtils.pad(1);
    String eliminationQuery =
        """
                    {
                        "$roots": [],
                        "$query": [
                          {
                            "$exists": "#id"
                          }
                        ],
                        "$filter": {}
                    }
                    """;

    ResponseEntity<String> r0 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r0.getStatusCode(), TestUtils.getBody(r0));
    String requestId2 = r0.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId2, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r0));

    String query =
        """
                {
                  "$query" : {},
                  "$filter" : {
                    "$offset" : 0,
                    "$limit" : 20,
                    "$orderby" : {
                      "#id" : 1
                    }
                  },
                  "$projection" : {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterDetails(tenant, query),
                r ->
                    r.getBody() != null
                        && r.getBody().hits().size() == 1
                        && "STORED_AND_UPDATED"
                            .equals(r.getBody().results().getFirst().get("Status").asText()));

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(1, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(2, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("SubmissionAgency").asText(), TestUtils.getBody(r2));
    assertEquals("STORED_AND_UPDATED", result.get("Status").asText(), TestUtils.getBody(r2));
    assertEquals("INGEST", result.get("OpType").asText(), TestUtils.getBody(r2));

    JsonNode totalObjectGroups = result.get("TotalObjectGroups");
    assertEquals(3, totalObjectGroups.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-3, totalObjectGroups.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjectGroups.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(3, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-3, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalObjects = result.get("TotalObjects");
    assertEquals(8, totalObjects.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-8, totalObjects.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjects.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode event0 = result.get("Events").get(0);
    assertEquals("INGEST", event0.get("OpType").asText(), TestUtils.getBody(r2));
    assertEquals(3, event0.get("Gots").asLong(), TestUtils.getBody(r2));
    assertEquals(3, event0.get("Units").asLong(), TestUtils.getBody(r2));
    assertEquals(8, event0.get("Objects").asLong(), TestUtils.getBody(r2));

    JsonNode event1 = result.get("Events").get(1);
    assertEquals("ELIMINATION", event1.get("OpType").asText(), TestUtils.getBody(r2));
    assertEquals(-3, event1.get("Gots").asLong(), TestUtils.getBody(r2));
    assertEquals(-3, event1.get("Units").asLong(), TestUtils.getBody(r2));
    assertEquals(-8, event1.get("Objects").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void searchForAccessionSummary(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[3];
    Arrays.fill(sips, 0, sips.length, sip);
    Scenario.uploadSips(restClient, tenant, tmpDir, sips);

    String query =
        """
                {
                  "$query" : {},
                  "$filter" : {
                    "$offset" : 0,
                    "$limit" : 20,
                    "$orderby" : {
                      "#id" : 1
                    }
                  },
                  "$projection" : {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterSummary(tenant, query),
                r ->
                    r.getBody() != null
                        && !r.getBody().results().isEmpty()
                        && r.getBody().results().getFirst().get("#version").asLong() == 3);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(1, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(3, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));

    JsonNode totalObjectGroups = result.get("TotalObjectGroups");
    assertEquals(9, totalObjectGroups.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjectGroups.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(9, totalObjectGroups.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(9, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(9, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalObjects = result.get("TotalObjects");
    assertEquals(24, totalObjects.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjects.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(24, totalObjects.get("remained").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void searchForAccessionSummary2(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Scenario.uploadSip(restClient, tenant, tmpDir, SipFactory.createComplexSip(tmpDir, 1));
    Scenario.uploadSip(restClient, tenant, tmpDir, SipFactory.createComplexSip(tmpDir, 2));

    String query =
        """
                    {
                      "$query" : {},
                      "$filter" : {
                        "$offset" : 0,
                        "$limit" : 20,
                        "$orderby" : {
                          "#id" : 1
                        }
                      },
                      "$projection" : {}
                    }
                    """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterSummary(tenant, query),
                r -> r.getBody() != null && r.getBody().hits().size() == 2);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(2, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(1, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(3, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(3, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));

    result = result1.results().get(1);
    assertEquals(1, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000002", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));

    totalUnits = result.get("TotalUnits");
    assertEquals(3, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(3, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void searchForAccessionSummaryAfterElimination(@TempDir Path tmpDir)
      throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[3];
    Arrays.fill(sips, 0, sips.length, sip);
    Scenario.uploadSips(restClient, tenant, tmpDir, sips);

    // Eliminate
    String acIdentifier = "AC-" + TestUtils.pad(1);
    String eliminationQuery =
        """
                        {
                            "$roots": [],
                            "$query": [
                              {
                                "$exists": "#version"
                              }
                            ],
                            "$filter": {}
                        }
                        """;

    ResponseEntity<String> r0 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r0.getStatusCode(), TestUtils.getBody(r0));
    String requestId2 = r0.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId2, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r0));

    String query =
        """
                    {
                      "$query" : {},
                      "$filter" : {
                        "$offset" : 0,
                        "$limit" : 20,
                        "$orderby" : {
                          "#id" : 1
                        }
                      },
                      "$projection" : {}
                    }
                    """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        Awaitility.await()
            .until(
                () -> restClient.searchAccessionRegisterSummary(tenant, query),
                r ->
                    r.getBody() != null
                        && !r.getBody().results().isEmpty()
                        && r.getBody().results().getFirst().get("#version").asLong() == 4);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> result1 = r2.getBody();
    assertNotNull(result1, TestUtils.getBody(r2));
    assertEquals(1, result1.hits().size(), TestUtils.getBody(r2));

    JsonNode result = result1.results().getFirst();
    assertEquals(4, result.get("#version").asLong(), TestUtils.getBody(r2));
    assertEquals("AGENCY-000001", result.get("OriginatingAgency").asText(), TestUtils.getBody(r2));

    JsonNode totalObjectGroups = result.get("TotalObjectGroups");
    assertEquals(9, totalObjectGroups.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-9, totalObjectGroups.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjectGroups.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalUnits = result.get("TotalUnits");
    assertEquals(9, totalUnits.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-9, totalUnits.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalUnits.get("remained").asLong(), TestUtils.getBody(r2));

    JsonNode totalObjects = result.get("TotalObjects");
    assertEquals(24, totalObjects.get("ingested").asLong(), TestUtils.getBody(r2));
    assertEquals(-24, totalObjects.get("deleted").asLong(), TestUtils.getBody(r2));
    assertEquals(0, totalObjects.get("remained").asLong(), TestUtils.getBody(r2));
  }
}
