/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.admin.domain.report.EliminationReport;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class ArchiveEliminateIT extends BaseIT {

  final int identifier = 1;
  final String acIdentifier = "AC-" + TestUtils.pad(identifier);

  private UserDto userDto;

  @BeforeAll
  void beforeAll() throws IOException, ParsingException {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void eliminateComplexSipTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[10];
    Arrays.fill(sips, 0, sips.length, sip);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sips);
    List<Long> id1s = ids.stream().map(m -> m.get("UNIT_ID1")).toList();
    // Utils.sleep(1000);

    String searchQuery =
        """
                {
                  "$roots":[ ],
                  "$query":[
                    {
                      "$match":{
                        "Title":"%s"
                      }
                    }
                  ],
                  "$filter":{ },
                  "$projection":{ }
                }
                """;

    String searchQuery1 = searchQuery.formatted("MyTitle1");
    String searchQuery2 = searchQuery.formatted("MyTitle2");
    String searchQuery3 = searchQuery.formatted("MyTitle3");

    // Execute tests
    eliminateComplexSip1(tenant, tmpDir, id1s, searchQuery1, searchQuery2, searchQuery3);
    eliminateComplexSip2(tenant, tmpDir, id1s, searchQuery1, searchQuery2, searchQuery3);
    eliminateComplexSip3(tenant, tmpDir, searchQuery1, searchQuery2, searchQuery3);
  }

  private void eliminateComplexSip1(
      Long tenant,
      Path tmpDir,
      List<Long> id1s,
      String searchQuery1,
      String searchQuery2,
      String searchQuery3)
      throws IOException {

    // Eliminate
    String eliminationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                          "$and": [
                            { "$match": { "Title": "MyTitle1" } },
                            { "$in": { "#id": [ "%s", "%s", "%s"] } }
                         ]
                     }
                   ],
                   "$filter": {}
                }
                """
            .formatted(id1s.get(0), id1s.get(1), id1s.get(2));

    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r2));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    EliminationReport report = JsonService.to(repPath, EliminationReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(requestId, report.operationId().toString());
    assertEquals(ReportType.ELIMINATION, report.type());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(9, report.units().size());

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery1);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> remainingUnits3 = r3.getBody().results();
    assertEquals(7, remainingUnits3.size(), TestUtils.getBody(r3));
    for (JsonNode unit : remainingUnits3) {
      assertEquals("MyTitle1", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r4 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery2);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    assertNotNull(r4.getBody());
    List<JsonNode> remainingUnits4 = r4.getBody().results();
    assertEquals(7, remainingUnits4.size(), TestUtils.getBody(r4));
    for (JsonNode unit : remainingUnits4) {
      assertEquals("MyTitle2", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r5 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery3);
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
    assertNotNull(r5.getBody());
    List<JsonNode> remainingUnits5 = r5.getBody().results();
    assertEquals(7, remainingUnits5.size(), TestUtils.getBody(r5));
    for (JsonNode unit : remainingUnits5) {
      assertEquals("MyTitle3", unit.get("Title").asText());
    }
  }

  private void eliminateComplexSip2(
      Long tenant,
      Path tmpDir,
      List<Long> id1s,
      String searchQuery1,
      String searchQuery2,
      String searchQuery3)
      throws IOException {

    // Eliminate
    String eliminationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                          "$and": [
                            { "$match": { "Title": "MyTitle2" } },
                            { "$in": { "#unitups": [ "%s", "%s", "%s", "%s"] } }
                         ]
                     }
                   ],
                   "$filter": {}
                }
                """
            .formatted(id1s.get(3), id1s.get(4), id1s.get(5), id1s.get(6));

    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r2));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    EliminationReport report = JsonService.to(repPath, EliminationReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(requestId, report.operationId().toString());
    assertEquals(ReportType.ELIMINATION, report.type());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(4, report.units().size());

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery1);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> remainingUnits3 = r3.getBody().results();
    assertEquals(7, remainingUnits3.size(), TestUtils.getBody(r3));
    for (JsonNode unit : remainingUnits3) {
      assertEquals("MyTitle1", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r4 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery2);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    assertNotNull(r4.getBody());
    List<JsonNode> remainingUnits4 = r4.getBody().results();
    assertEquals(3, remainingUnits4.size(), TestUtils.getBody(r4));
    for (JsonNode unit : remainingUnits4) {
      assertEquals("MyTitle2", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r5 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery3);
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
    assertNotNull(r5.getBody());
    List<JsonNode> remainingUnits5 = r5.getBody().results();
    assertEquals(7, remainingUnits5.size(), TestUtils.getBody(r5));
    for (JsonNode unit : remainingUnits5) {
      assertEquals("MyTitle3", unit.get("Title").asText());
    }
  }

  private void eliminateComplexSip3(
      Long tenant, Path tmpDir, String searchQuery1, String searchQuery2, String searchQuery3)
      throws IOException {

    // Eliminate
    String eliminationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                           "$match": { "Title": "MyTitle3" }
                     }
                   ],
                   "$filter": {}
                }
                """;

    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r2));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    EliminationReport report = JsonService.to(repPath, EliminationReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(requestId, report.operationId().toString());
    assertEquals(ReportType.ELIMINATION, report.type());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(7, report.units().size());

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery1);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> remainingUnits3 = r3.getBody().results();
    assertEquals(7, remainingUnits3.size(), TestUtils.getBody(r3));
    for (JsonNode unit : remainingUnits3) {
      assertEquals("MyTitle1", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r4 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery2);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    assertNotNull(r4.getBody());
    List<JsonNode> remainingUnits4 = r4.getBody().results();
    assertEquals(3, remainingUnits4.size(), TestUtils.getBody(r4));
    for (JsonNode unit : remainingUnits4) {
      assertEquals("MyTitle2", unit.get("Title").asText());
    }

    ResponseEntity<SearchResult<JsonNode>> r5 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery3);
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
    assertNotNull(r5.getBody());
    List<JsonNode> remainingUnits5 = r5.getBody().results();
    assertEquals(0, remainingUnits5.size(), TestUtils.getBody(r5));
    for (JsonNode unit : remainingUnits5) {
      assertEquals("MyTitle3", unit.get("Title").asText());
    }
  }

  @Test
  void eliminateSimpleSip(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);
    ArchiveTransfer simpleSip = SipFactory.createSimpleSip(tmpDir, 1);
    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, simpleSip);
    Long id1 = ids.get("UNIT_ID1");

    // Eliminate
    String eliminationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                         "$eq": { "#id": "%s" }
                     }
                   ],
                   "$filter": {}
                }
                """
            .formatted(id1);

    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, statusDto.status(), TestUtils.getBody(r2));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.downloadReport(tenant, requestId, repPath));
    assertFalse(Files.exists(repPath));
    assertEquals(HttpStatusCode.valueOf(404), thrown.getStatusCode());
  }

  @Test
  void eliminateSimpleSip2(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);
    ArchiveTransfer simpleSip = SipFactory.createSimpleSipWithNowDate(tmpDir, 1);
    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, simpleSip);
    Long id3 = ids.get("UNIT_ID3");

    // Eliminate
    String eliminationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                         "$eq": { "#id": "%s" }
                     }
                   ],
                   "$filter": {}
                }
                """
            .formatted(id3);

    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliminationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, statusDto.status(), TestUtils.getBody(r2));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.downloadReport(tenant, requestId, repPath));
    assertFalse(Files.exists(repPath));
    assertEquals(HttpStatusCode.valueOf(404), thrown.getStatusCode());
  }

  @Test
  void eliminateSimpleSip3(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);
    Map<String, Long> ids =
        Scenario.uploadSip(
            restClient, tenant, tmpDir, SipFactory.createSimpleSipWithPastDate(tmpDir, 1));

    // Eliminate
    String eliminationQuery =
        """
                {
                    "$roots": [],
                    "$query": [
                      {
                          "$eq": { "#id": "%s" }
                      }
                    ],
                    "$filter": {}
                }
                """;

    String eliQuery = eliminationQuery.formatted(ids.get("UNIT_ID3"));
    ResponseEntity<String> ro = restClient.eliminateArchive(tenant, acIdentifier, eliQuery);
    assertEquals(HttpStatus.ACCEPTED, ro.getStatusCode(), TestUtils.getBody(ro));
    String requestId = ro.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(ro));

    // Get elimination report
    Path repPath = tmpDir.resolve(requestId + ".elimination_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    EliminationReport report = JsonService.to(repPath, EliminationReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(requestId, report.operationId().toString());
    assertEquals(ReportType.ELIMINATION, report.type());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(ids.get("UNIT_ID3"), report.units().getFirst());

    // Search units
    String searchQuery =
        """
                    {
                      "$roots": [],
                      "$query": [
                        {
                          "$eq": {
                            "#id": "%s"
                          }
                        }
                      ],
                      "$filter": {},
                      "$projection": {}
                    }
                    """;

    String query0 = searchQuery.formatted(ids.get("UNIT_ID3"));
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, query0);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    SearchResult<JsonNode> searchResult3 = r3.getBody();
    assertNotNull(searchResult3);
    assertEquals(0, searchResult3.results().size(), TestUtils.getBody(r3));

    String query1 = searchQuery.formatted(ids.get("UNIT_ID1"));
    ResponseEntity<SearchResult<JsonNode>> r1 =
        restClient.searchArchive(tenant, acIdentifier, query1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));
    SearchResult<JsonNode> searchResult1 = r1.getBody();
    assertNotNull(searchResult1);
    assertEquals(1, searchResult1.results().size(), TestUtils.getBody(r1));

    String query2 = searchQuery.formatted(ids.get("UNIT_ID2"));
    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, query2);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SearchResult<JsonNode> searchResult2 = r2.getBody();
    assertNotNull(searchResult2);
    assertEquals(1, searchResult2.results().size(), TestUtils.getBody(r2));
  }

  @Test
  void eliminateReclassifiedSip(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);
    Map<String, Long> ids1 =
        Scenario.uploadSip(restClient, tenant, tmpDir, SipFactory.createSimpleSip(tmpDir, 1));
    Map<String, Long> ids2 =
        Scenario.uploadSip(
            restClient, tenant, tmpDir, SipFactory.createSimpleSipWithNowDate(tmpDir, 1));
    Map<String, Long> ids3 =
        Scenario.uploadSip(
            restClient, tenant, tmpDir, SipFactory.createSimpleSipWithPastDate(tmpDir, 1));

    // Eliminate
    String eliminationQuery =
        """
                {
                    "$roots": [],
                    "$query": [
                      {
                          "$eq": { "#id": "%s" }
                      }
                    ],
                    "$filter": {}
                }
                """;

    String eliQuery1 = eliminationQuery.formatted(ids1.get("UNIT_ID3"));
    ResponseEntity<String> r1 = restClient.eliminateArchive(tenant, acIdentifier, eliQuery1);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId1 = r1.getHeaders().getFirst(X_REQUEST_ID);

    String eliQuery2 = eliminationQuery.formatted(ids2.get("UNIT_ID2"));
    ResponseEntity<String> r2 = restClient.eliminateArchive(tenant, acIdentifier, eliQuery2);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId2 = r2.getHeaders().getFirst(X_REQUEST_ID);

    // No appraisal rule
    OperationStatusDto status1 =
        restClient.waitForOperationStatus(tenant, requestId1, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, status1.status(), TestUtils.getBody(r1));

    // MaxEndDate is bad
    OperationStatusDto status2 =
        restClient.waitForOperationStatus(tenant, requestId2, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, status2.status(), TestUtils.getBody(r2));

    // Reclassify
    String reclassificationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$eq": { "#id": "%s" }
                     }
                   ],
                   "$filter": {},
                   "$action": [
                        { "$add": { "#unitup": "%s" }
                        }
                    ]
                }
                """;

    // Reclassify
    String reQuery3 = reclassificationQuery.formatted(ids1.get("UNIT_ID1"), ids3.get("UNIT_ID3"));
    ResponseEntity<String> r3 = restClient.reclassifyArchive(tenant, acIdentifier, reQuery3);
    assertEquals(HttpStatus.ACCEPTED, r3.getStatusCode(), TestUtils.getBody(r3));
    String requestId3 = r3.getHeaders().getFirst(X_REQUEST_ID);

    String reQuery4 = reclassificationQuery.formatted(ids2.get("UNIT_ID1"), ids3.get("UNIT_ID3"));
    ResponseEntity<String> r4 = restClient.reclassifyArchive(tenant, acIdentifier, reQuery4);
    assertEquals(HttpStatus.ACCEPTED, r4.getStatusCode(), TestUtils.getBody(r4));
    String requestId4 = r4.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto status3 =
        restClient.waitForOperationStatus(tenant, requestId3, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, status3.status(), TestUtils.getBody(r3));

    OperationStatusDto status4 =
        restClient.waitForOperationStatus(tenant, requestId4, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, status4.status(), TestUtils.getBody(r4));

    // Eliminate again
    ResponseEntity<String> r5 = restClient.eliminateArchive(tenant, acIdentifier, eliQuery1);
    assertEquals(HttpStatus.ACCEPTED, r5.getStatusCode(), TestUtils.getBody(r5));
    String requestId5 = r5.getHeaders().getFirst(X_REQUEST_ID);

    ResponseEntity<String> r6 = restClient.eliminateArchive(tenant, acIdentifier, eliQuery2);
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));
    String requestId6 = r6.getHeaders().getFirst(X_REQUEST_ID);

    String eliQuery3 = eliminationQuery.formatted(ids2.get("UNIT_ID1"));
    ResponseEntity<String> r7 = restClient.eliminateArchive(tenant, acIdentifier, eliQuery3);
    assertEquals(HttpStatus.ACCEPTED, r7.getStatusCode(), TestUtils.getBody(r7));
    String requestId7 = r7.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto status5 =
        restClient.waitForOperationStatus(tenant, requestId5, 30, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, status5.status(), TestUtils.getBody(r5));

    OperationStatusDto status6 =
        restClient.waitForOperationStatus(tenant, requestId6, 30, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, status6.status(), TestUtils.getBody(r6));

    OperationStatusDto status7 =
        restClient.waitForOperationStatus(tenant, requestId7, 30, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, status7.status(), TestUtils.getBody(r7));

    // Get elimination report
    Path repPath5 = tmpDir.resolve(requestId5 + ".elimination_report");
    restClient.downloadReport(tenant, requestId5, repPath5);
    EliminationReport report5 = JsonService.to(repPath5, EliminationReport.class);
    assertEquals(tenant, report5.tenant());
    assertEquals(requestId5, report5.operationId().toString());
    assertEquals(ids1.get("UNIT_ID3"), report5.units().getFirst());

    // Search
    String searchQuery =
        """
                {
                  "$roots": [],
                  "$query": [
                    {
                      "$eq": {
                        "#id": "%s"
                      }
                    }
                  ],
                  "$filter": {},
                  "$projection": {}
                }
                """;

    String seQuery1 = searchQuery.formatted(ids1.get("UNIT_ID3"));
    ResponseEntity<SearchResult<JsonNode>> r8 =
        restClient.searchArchive(tenant, acIdentifier, seQuery1);
    assertEquals(HttpStatus.OK, r8.getStatusCode(), TestUtils.getBody(r8));
    SearchResult<JsonNode> searchResult3 = r8.getBody();
    assertNotNull(searchResult3);
    assertEquals(0, searchResult3.results().size(), TestUtils.getBody(r8));
  }
}
