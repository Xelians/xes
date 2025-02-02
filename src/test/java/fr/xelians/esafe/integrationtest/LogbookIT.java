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
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.operation.dto.vitam.VitamExternalEventDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import nu.xom.ParsingException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class LogbookIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);

  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void logbookLifecycles(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, sip);

    String updateQuery =
        """
                     {
                       "$roots": [],
                       "$type": "DOCTYPE-000001",
                       "$query": [
                         {
                           "$match": { "Title": "MyTitle2" },
                           "$depth": 2
                         }
                       ],
                       "$filter": {},
                       "$action": [
                            { "$set": { "Code": 98766 }},
                            { "$set": { "Directeur":
                                            {
                                              "Age": 799,
                                              "Nom": "Martin",
                                              "Prenom": "Robert"
                                            }
                                      }
                            }
                        ]
                    }
                    """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, ope.status(), TestUtils.getBody(r1));

    Long id1 = ids.get("UNIT_ID2");
    ResponseEntity<JsonNode> r2 = restClient.getLogbookUnitLifecycles(tenant, acIdentifier, id1);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    ResponseEntity<JsonNode> r3 = restClient.getLogbookObjectLifecycles(tenant, acIdentifier, id1);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    // TODO Add some assertions
  }

  @Test
  void logbookOperationIngestComplexSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createComplexSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));

    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    // Wait for Elastic to index
    VitamLogbookOperationDto logbookOperation =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getVitamLogbookOperation(tenant, requestId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(logbookOperation);
    assertEquals(operation.id(), logbookOperation.getId());
    assertEquals(operation.tenant(), logbookOperation.getTenant());
    assertTrue(operation.type().toString().startsWith(logbookOperation.getEvTypeProc()));

    String query =
        """
                 {
                   "$query": [
                     {
                       "$eq": { "Type": "INGEST_ARCHIVE" }
                     }
                   ],
                   "$filter": {},
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r3 =
        Awaitility.await()
            .until(
                () -> restClient.searchVitamLogbookOperation(tenant, query),
                r -> r.getBody() != null && r.getBody().hits().size() > 0);

    SearchResult<JsonNode> result = r3.getBody();
    assertNotNull(result, TestUtils.getBody(r3));
    assertTrue(result.hits().size() > 0, TestUtils.getBody(r3));
    assertEquals(
        tenant, result.results().getFirst().get("#tenant").asLong(), TestUtils.getBody(r3));

    ResponseEntity<SearchResult<LogbookOperationDto>> r4 =
        Awaitility.await()
            .until(
                () -> restClient.searchLogbookOperation(tenant, query),
                r -> r.getBody() != null && r.getBody().hits().size() > 0);

    SearchResult<LogbookOperationDto> result4 = r4.getBody();
    assertNotNull(result4, TestUtils.getBody(r4));
    assertTrue(result4.hits().size() > 0, TestUtils.getBody(r4));
    assertEquals(tenant, result4.results().getFirst().tenant(), TestUtils.getBody(r4));
    assertEquals(
        OperationType.INGEST_ARCHIVE, result4.results().getFirst().type(), TestUtils.getBody(r4));
    assertEquals("OK", result4.results().getFirst().outcome(), TestUtils.getBody(r4));
  }

  @Test
  void logbookOperationUpdateSip(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createUpdateOperationSip(tmpDir, 1, systemId), sipPath);

    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));

    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation =
        restClient.waitForOperation(tenant, requestId, 10, RestClient.OP_FINAL);
    // TODO Get Sip and assert parent == systemId

    // Get operation in logbook index
    VitamLogbookOperationDto logbookOperation =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getVitamLogbookOperation(tenant, requestId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(logbookOperation);
    assertEquals(operation.id(), logbookOperation.getId());
    assertEquals(operation.tenant(), logbookOperation.getTenant());
    assertTrue(operation.type().toString().startsWith(logbookOperation.getEvTypeProc()));
  }

  @Test
  void logbookOperationIngestSimpleSip(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto(3));
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    IngestContractDto ic2 = DtoFactory.createIngestContractDto(3);
    ic2.setLinkParentId(systemId);
    ResponseEntity<List<IngestContractDto>> r2 = restClient.createIngestContract(tenant, ic2);
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 3), sipPath);

    // Upload Sip
    ResponseEntity<Void> r3 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r3.getStatusCode(), TestUtils.getBody(r3));

    // Wait for async ingest operation from db
    String requestId = r3.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation =
        restClient.waitForOperation(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());
    // TODO Get Sip and assert parent == systemId

    // Get operation in logbook index
    VitamLogbookOperationDto vitamLogbookOperation =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getVitamLogbookOperation(tenant, requestId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(vitamLogbookOperation);
    assertEquals(operation.id(), vitamLogbookOperation.getId());
    assertEquals(operation.tenant(), vitamLogbookOperation.getTenant());
    assertTrue(operation.type().toString().startsWith(vitamLogbookOperation.getEvTypeProc()));

    LogbookOperationDto logbookOperation =
        Awaitility.await()
            .until(
                () -> restClient.getLogbookOperation(tenant, requestId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(logbookOperation);
    assertEquals(vitamLogbookOperation.getId(), logbookOperation.id());
    assertEquals(vitamLogbookOperation.getEvId(), logbookOperation.id());
    assertEquals(vitamLogbookOperation.getTenant(), logbookOperation.tenant());
    assertTrue(
        logbookOperation.type().toString().startsWith(vitamLogbookOperation.getEvTypeProc()));
    assertEquals(vitamLogbookOperation.getEvIdAppSession(), logbookOperation.applicationId());
    assertEquals(vitamLogbookOperation.getOutcome(), logbookOperation.outcome());
    assertEquals(vitamLogbookOperation.getAgId(), logbookOperation.userIdentifier());
  }

  @Nested
  class RootAdminTest {
    @BeforeEach
    void beforeEach() {
      signInAsRootAdmin();
    }

    @Test
    void emptyQuerySearchTest() {
      Long tenant = nextTenant();

      VitamExternalEventDto dto = DtoFactory.createExternalOperationDto();
      ResponseEntity<JsonNode> r1 = restClient.createExternalOperation(tenant, dto);
      assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

      String query =
          """
                {
                  "$filter": {
                     "$offset": 0,
                     "$limit": 20,
                     "$orderby": { "#id": 1 }
                  },
                  "$projection": {}
                }
                """;

      ResponseEntity<SearchResult<JsonNode>> r2 =
          Awaitility.await()
              .until(
                  () -> restClient.searchVitamLogbookOperation(tenant, query),
                  r -> r.getBody() != null && r.getBody().hits().size() > 0);

      SearchResult<JsonNode> result = r2.getBody();
      assertNotNull(result, TestUtils.getBody(r2));

      List<JsonNode> results = result.results();
      assertFalse(results.isEmpty(), TestUtils.getBody(r2));
      assertEquals(tenant, results.getFirst().get("#tenant").asLong(), TestUtils.getBody(r2));
      assertEquals(
          "EXT_UPDATE_USER", results.getFirst().get("evType").asText(), TestUtils.getBody(r2));
      assertEquals("OK", results.getFirst().get("outcome").asText(), TestUtils.getBody(r2));
    }

    @Test
    void externalOperationTest() {
      Long tenant = nextTenant();

      VitamExternalEventDto dto = DtoFactory.createExternalOperationDto();
      ResponseEntity<JsonNode> r1 = restClient.createExternalOperation(tenant, dto);
      assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

      String query =
          """
              {
                 "$query": [
                   {
                     "$and": [
                         {"$eq": { "Type": "EXTERNAL" }},
                         {"$eq": { "ObjectIdentifier": "12678" }}
                       ]
                   }
                 ],
                 "$filter": {},
                 "$projection": {}
              }
              """;

      ResponseEntity<SearchResult<JsonNode>> r2 =
          Awaitility.await()
              .until(
                  () -> restClient.searchVitamLogbookOperation(tenant, query),
                  r -> r.getBody() != null && r.getBody().hits().size() > 0);

      SearchResult<JsonNode> result = r2.getBody();
      assertNotNull(result, TestUtils.getBody(r2));

      List<JsonNode> results = result.results();
      assertFalse(results.isEmpty(), TestUtils.getBody(r2));
      assertEquals(tenant, results.getFirst().get("#tenant").asLong(), TestUtils.getBody(r2));
      assertEquals(
          "EXT_UPDATE_USER", results.getFirst().get("evType").asText(), TestUtils.getBody(r2));
      assertEquals("OK", results.getFirst().get("outcome").asText(), TestUtils.getBody(r2));
    }
  }
}
