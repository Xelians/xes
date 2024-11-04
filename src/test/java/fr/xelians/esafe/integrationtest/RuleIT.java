/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.admin.domain.report.RulesReport;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.dto.RuleDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nu.xom.ParsingException;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class RuleIT extends BaseIT {

  private static final Path RULE1 = Paths.get(ItInit.RULE + "OK_regles_CSV.csv");

  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void createCsvRulesTest() throws IOException {
    Path dir = Paths.get(ItInit.RULE);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".csv")) {
      ResponseEntity<List<RuleDto>> response = restClient.createCsvRule(nextTenant(), path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  @Test
  void createBadCsvRulesTest() throws IOException {
    Path dir = Paths.get(ItInit.RULE);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_", ".csv")) {
      Long tenant = nextTenant();
      HttpClientErrorException thrown =
          assertThrows(
              HttpClientErrorException.class, () -> restClient.createCsvRule(tenant, path));
      assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
    }
  }

  @Test
  void createRuleTest() {
    Long tenant = nextTenant();
    RuleDto ruleDto = DtoFactory.createAppraisalRule(1);
    ResponseEntity<List<RuleDto>> r1 = restClient.createRule(tenant, ruleDto);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<RuleDto> r2 = restClient.getRuleByIdentifier(tenant, ruleDto.getIdentifier());
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    RuleDto ruleDto2 = r2.getBody();
    assertNotNull(ruleDto2);
    assertEquals(ruleDto.getIdentifier(), ruleDto2.getIdentifier(), TestUtils.getBody(r2));
    assertEquals(ruleDto.getName(), ruleDto2.getName(), TestUtils.getBody(r2));

    ResponseEntity<PageResult<RuleDto>> r3 = restClient.getRules(tenant, Collections.emptyMap());
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
  }

  @Test
  void findCsvRulesTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, RULE1);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<String> r2 = restClient.getCsvRules(tenant);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  }

  @Test
  void should_reimport_rules_csv_should_erase_and_recreate_rules() throws IOException {
    var tenant = nextTenant();
    Path path = Paths.get(ItInit.RULE + "OK_regles_OK-complete.csv");

    ResponseEntity<List<RuleDto>> response1 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.CREATED, response1.getStatusCode(), TestUtils.getBody(response1));

    path = Paths.get(ItInit.RULE + "OK_regles_CSV.csv");
    ResponseEntity<List<RuleDto>> response2 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.CREATED, response2.getStatusCode(), TestUtils.getBody(response2));
  }

  @Test
  void searchRulesTest() throws IOException {
    Long tenant = nextTenant();

    String query =
        """
             {
               "$query": [
                  {
                     "$neq": { "Identifier": "BAD_666" }
                 }
               ],
               "$filter": {
                   "$offset": 10,
                   "$limit": 12,
                   "$orderby": { "Identifier": 1 }
              },
               "$projection": {"$fields": { "Identifier": 1, "LastUpdate": 1, "Status": 1 }}
            }
            """;

    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, RULE1);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchRules(tenant, query);
    SearchResult<JsonNode> outputDtos2 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos2);
    assertEquals(12, outputDtos2.results().size(), TestUtils.getBody(r3));
    assertEquals(62, outputDtos2.hits().total(), TestUtils.getBody(r3));
  }

  @Test
  void delete_csv_rules_should_not_fail(@TempDir Path tmpDir) throws IOException, ParsingException {
    var tenant = nextTenant();

    Scenario.createScenario02(restClient, tenant, userDto);
    String acIdentifier = "AC-" + TestUtils.pad(1);
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);

    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, sip);
    for (var id : ids.values()) {
      Awaitility.given()
          .ignoreException(HttpClientErrorException.NotFound.class)
          .await()
          .until(
              () -> restClient.getArchiveUnit(tenant, acIdentifier, id),
              r -> r.getStatusCode() == HttpStatus.OK);
    }

    Path path = Paths.get(ItInit.RULE + "OK_regles_OK-complete.csv");
    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));
  }

  //  @Test
  //  void delete_rules_should_success() throws IOException {
  //    var tenant = nextTenant();
  //
  //    Scenario.createScenario02(restClient, tenant, userDto);
  //
  //    String query =
  //        """
  //                 {
  //                   "$query": [ { "$eq": { "Identifier": "CLASSIFICATIONRULE-000004" } } ],
  //                   "$filter": { },
  //                   "$projection": {}
  //                }
  //                """;
  //
  //    ResponseEntity<SearchResult<JsonNode>> r0 = restClient.searchRules(tenant, query);
  //    assertEquals(HttpStatus.OK, r0.getStatusCode(), TestUtils.getBody(r0));
  //    assertEquals(1, r0.getBody().results().size());
  //
  //    ResponseEntity<Void> r1 = restClient.deleteRule(tenant, "CLASSIFICATIONRULE-000004");
  //    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));
  //
  //    ResponseEntity<SearchResult<JsonNode>> r2 = restClient.searchRules(tenant, query);
  //    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  //    assertEquals(1, r2.getBody().results().size());
  //    assertEquals("INACTIVE", r2.getBody().results().getFirst().get("Status").asText());
  //  }

  @Test
  void delete_rules_should_fail_again(@TempDir Path tmpDir) throws IOException, ParsingException {
    var tenant = nextTenant();

    Scenario.createScenario02(restClient, tenant, userDto);
    String acIdentifier = "AC-" + TestUtils.pad(1);
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);

    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, sip);
    for (var id : ids.values()) {
      Awaitility.given()
          .ignoreException(HttpClientErrorException.NotFound.class)
          .await()
          .until(
              () -> restClient.getArchiveUnit(tenant, acIdentifier, id),
              r -> r.getStatusCode() == HttpStatus.OK);
    }

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.deleteRule(tenant, "DISSEMINATIONRULE-000002"));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void update_rules_and_check_report(@TempDir Path tmpDir) throws IOException {

    var tenant = nextTenant();
    Path path = Paths.get(ItInit.RULE + "OK_regles_OK-complete.csv");

    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    Path repPath = tmpDir.resolve(requestId + ".rules_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    RulesReport report = JsonService.to(repPath, RulesReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(ReportType.RULES_REFERENTIAL, report.type());
    assertEquals(requestId, report.operation().evId());
    assertEquals("CREATE_RULE", report.operation().evType());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(160, report.insertedRules().size());

    path = Paths.get(ItInit.RULE + "OK_regles_CSV.csv");
    ResponseEntity<List<RuleDto>> r2 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    repPath = tmpDir.resolve(requestId + ".rules_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));
    report = JsonService.to(repPath, RulesReport.class);
    assertEquals(tenant, report.tenant());
    assertEquals(ReportType.RULES_REFERENTIAL, report.type());
    assertEquals(requestId, report.operation().evId());
    assertEquals("CREATE_RULE", report.operation().evType());
    assertEquals(ReportStatus.OK, report.status());
    assertEquals(62, report.insertedRules().size());
    assertEquals(102, report.deletedRules().size());
  }
}
