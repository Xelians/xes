/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
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
import org.springframework.http.ResponseEntity;

class ArchiveRuleUpdateIT extends BaseIT {

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
  void updateRuleTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    // Init
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[10];
    Arrays.fill(sips, 0, sips.length, sip);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sips);
    List<Long> id1s = ids.stream().map(m -> m.get("UNIT_ID1")).toList();
    // Utils.sleep(1000);

    // Execute tests
    updateRule1(tenant, tmpDir, id1s);
    updateRule2(tenant, tmpDir, id1s);
    updateRule3(tenant, tmpDir, id1s);
  }

  private void updateRule1(Long tenant, Path tmpDir, List<Long> id1s) throws IOException {

    String rule1 = "APPRAISALRULE-" + TestUtils.pad(1);
    String rule2 = "APPRAISALRULE-" + TestUtils.pad(2);
    String rule3 = "APPRAISALRULE-" + TestUtils.pad(3);

    // Eliminate
    String updateQuery =
        """
                   {
                   "dslRequest": {
                     "$roots": [],
                     "$query": [ { "$match": { "Title": "MyTitle1" } } ]
                   },
                   "ruleActions": {
                     "delete": [ {
                        "AppraisalRule": {
                          "Rules": [ { "Rule": "%s" },  { "Rule": "%s" },  { "Rule": "%s" } ]
                    } } ]
                   }
                  }
                  """
            .formatted(rule1, rule2, rule3);

    ResponseEntity<String> r1 = restClient.updateRulesArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r1));

    // Search for units
    String searchQuery =
        """
                  {
                    "$roots":[ ],
                    "$query": [ { "$match": { "Title": "%s" } } ],
                    "$filter": { },
                    "$projection":{ }
                  }
                  """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle1"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    assertNotNull(r2.getBody());
    for (JsonNode unit : r2.getBody().results()) {
      assertEquals("MyTitle1", unit.get("Title").asText());

      JsonNode arules = unit.get("#management").get("AppraisalRule");
      assertEquals(0, arules.get("Rules").size());

      JsonNode endDate = arules.get("EndDate");
      assertNull(endDate);
    }

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle2"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());

    for (JsonNode unit : r3.getBody().results()) {
      assertEquals("MyTitle2", unit.get("Title").asText());

      JsonNode inheritedFinalAction =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("FinalAction");
      assertEquals("Destroy", inheritedFinalAction.get(0).asText());

      JsonNode maxEndDate =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("MaxEndDate");
      assertNull(maxEndDate);
    }
  }

  private void updateRule2(Long tenant, Path tmpDir, List<Long> id1s) throws IOException {

    String rule1 = "APPRAISALRULE-" + TestUtils.pad(1);
    String rule2 = "APPRAISALRULE-" + TestUtils.pad(2);
    String rule3 = "APPRAISALRULE-" + TestUtils.pad(3);

    // Eliminate
    String updateQuery =
        """
           {
           "dslRequest": {
             "$roots": [],
             "$query": [ { "$match": { "Title": "MyTitle1" } } ]
           },
           "ruleActions": {
             "add": [ {
                  "AppraisalRule": {
                    "FinalAction": "Destroy",
                    "Rules": [
                      {
                        "Rule": "%s",
                        "StartDate": "2011-11-14"
                      },
                      {
                        "Rule": "%s",
                        "StartDate": "2012-11-14"
                      },
                      {
                        "Rule": "%s",
                        "StartDate": "2013-11-14"
                      }
                    ]
                  }
               } ]
             }
          }
          """
            .formatted(rule1, rule2, rule3);

    ResponseEntity<String> r1 = restClient.updateRulesArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r1));

    // Search for units
    String searchQuery =
        """
          {
            "$roots":[ ],
            "$query": [ { "$match": { "Title": "%s" } } ],
            "$filter": { },
            "$projection":{ }
          }
          """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle1"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    assertNotNull(r2.getBody());

    for (JsonNode unit : r2.getBody().results()) {
      assertEquals("MyTitle1", unit.get("Title").asText());

      JsonNode rules = unit.get("#management").get("AppraisalRule").get("Rules");
      assertEquals(rule1, rules.get(0).get("Rule").asText());
      assertEquals("2021-11-14", rules.get(0).get("EndDate").asText());
      assertEquals(rule2, rules.get(1).get("Rule").asText());
      assertEquals("2022-11-14", rules.get(1).get("EndDate").asText());
      assertEquals(rule3, rules.get(2).get("Rule").asText());
      assertEquals("2023-11-14", rules.get(2).get("EndDate").asText());

      JsonNode endDate = unit.get("#management").get("AppraisalRule").get("EndDate");
      assertEquals("2023-11-14", endDate.asText());
      JsonNode finalAction = unit.get("#management").get("AppraisalRule").get("FinalAction");
      assertEquals("Destroy", finalAction.asText());

      JsonNode inheritedFinalAction =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("FinalAction");
      assertEquals("Destroy", inheritedFinalAction.get(0).asText());

      JsonNode maxEndDate =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("MaxEndDate");
      assertEquals("2023-11-14", maxEndDate.asText());
    }

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle2"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());

    for (JsonNode unit : r3.getBody().results()) {
      assertEquals("MyTitle2", unit.get("Title").asText());

      JsonNode inheritedFinalAction =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("FinalAction");
      assertEquals("Destroy", inheritedFinalAction.get(0).asText());

      JsonNode maxEndDate =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("MaxEndDate");
      assertEquals("2023-11-14", maxEndDate.asText());
    }
  }

  private void updateRule3(Long tenant, Path tmpDir, List<Long> id1s) throws IOException {

    String rule1 = "APPRAISALRULE-" + TestUtils.pad(1);
    String rule2 = "APPRAISALRULE-" + TestUtils.pad(2);
    String rule3 = "APPRAISALRULE-" + TestUtils.pad(3);

    // Eliminate
    String updateQuery =
        """
               {
               "dslRequest": {
                 "$roots": [],
                 "$query": [ { "$match": { "Title": "MyTitle1" } } ],
                 "$threshold": 10000
               },
               "ruleActions": {
                 "update": [ {
                      "AppraisalRule": {
                        "FinalAction": "Keep",
                        "Rules": [
                          {
                            "OldRule": "%s",
                            "StartDate": "2031-11-14"
                          },
                          {
                            "OldRule": "%s",
                            "StartDate": "2032-11-14"
                          },
                          {
                            "OldRule": "%s",
                            "DeleteStartDate": true
                          }
                        ]
                      }
                   } ]
                 }
              }
              """
            .formatted(rule1, rule2, rule3);

    ResponseEntity<String> r1 = restClient.updateRulesArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto statusDto =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, statusDto.status(), TestUtils.getBody(r1));

    // Search for units
    String searchQuery =
        """
              {
                "$roots":[ ],
                "$query": [ { "$match": { "Title": "%s" } } ],
                "$filter": { },
                "$projection":{ }
              }
              """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle1"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    assertNotNull(r2.getBody());

    for (JsonNode unit : r2.getBody().results()) {
      assertEquals("MyTitle1", unit.get("Title").asText());

      JsonNode rules = unit.get("#management").get("AppraisalRule").get("Rules");
      assertEquals(rule1, rules.get(0).get("Rule").asText());
      assertEquals("2041-11-14", rules.get(0).get("EndDate").asText());
      assertEquals(rule2, rules.get(1).get("Rule").asText());
      assertEquals("2042-11-14", rules.get(1).get("EndDate").asText());
      assertEquals(rule3, rules.get(2).get("Rule").asText());
      assertNull(rules.get(2).get("EndDate"));

      JsonNode endDate = unit.get("#management").get("AppraisalRule").get("EndDate");
      assertNull(endDate);
      JsonNode finalAction = unit.get("#management").get("AppraisalRule").get("FinalAction");
      assertEquals("Keep", finalAction.asText());

      JsonNode inheritedFinalAction =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("FinalAction");
      assertEquals("Keep", inheritedFinalAction.get(0).asText());

      JsonNode maxEndDate =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("MaxEndDate");
      assertNull(maxEndDate);
    }

    // Search for units
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery.formatted("MyTitle2"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());

    for (JsonNode unit : r3.getBody().results()) {
      assertEquals("MyTitle2", unit.get("Title").asText());

      JsonNode inheritedFinalAction =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("FinalAction");
      assertEquals("Keep", inheritedFinalAction.get(0).asText());

      JsonNode maxEndDate =
          unit.get("#computedInheritedRules").get("AppraisalRule").get("MaxEndDate");
      assertNull(maxEndDate);
    }
  }
}
