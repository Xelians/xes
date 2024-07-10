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
import fr.xelians.esafe.common.utils.DateUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.search.domain.dsl.bucket.*;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.model.ArchiveUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class ArchiveSearchIT extends BaseIT {

  private final List<String> requestIds = new ArrayList<>();
  private final List<Long> unitIds = new ArrayList<>();
  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private Long tenant;

  @BeforeAll
  void beforeAll(@TempDir Path tmpDir) throws IOException, ParsingException {
    SetupDto setupDto = setup();
    tenant = setupDto.tenant();
    UserDto userDto = setupDto.userDto();

    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("complex_sip.zip");
    List<String> ages = List.of("40", "12", "28", "20", "35", "17", "45", "67", "78", "33");
    ArchiveTransfer complexSip = SipFactory.createComplexSip(tmpDir, identifier);

    for (String age : ages) {
      complexSip
          .getArchiveUnits()
          .getFirst()
          .getArchiveUnits()
          .getFirst()
          .getElements("Directeur")
          .stream()
          .flatMap(a -> a.getElements().stream())
          .filter(e -> e.getName().equals("Age"))
          .forEach(e -> e.setValue(age));

      sedaService.write(complexSip, sipPath);
      ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
      assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      requestIds.add(requestId);
    }

    for (String requestId : requestIds) {
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, operation.status());

      Path atrPath = tmpDir.resolve(requestId + ".atr");
      restClient.downloadXmlAtr(tenant, requestId, atrPath);
      Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
      XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
      xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");
      addAllUnitIds(complexSip.getArchiveUnits(), rootElem, xc);
    }

    Utils.sleep(1000);
  }

  private void addAllUnitIds(List<ArchiveUnit> archiveUnits, Element rootElem, XPathContext xc) {
    for (ArchiveUnit au : archiveUnits) {
      Nodes nodes = rootElem.query("//ns:ArchiveUnit[@id='" + au.getId() + "']", xc);
      unitIds.add(Long.parseLong(nodes.get(0).getValue()));
      addAllUnitIds(au.getArchiveUnits(), rootElem, xc);
    }
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void queryObjectMatchOperatorTest() {
    String query =
        """
                     {
                       "$roots": [],
                       "$query": [
                         {
                           "$match": { "Title": "MyTitle1" },
                           "$depth": 1
                         }
                       ],
                       "$filter": {
                         "$limit": 100,
                         "$orderby": { "#score": -1, "#id": 1 }
                       },
                       "$filter": {},
                       "$projection": {}
                    }
                    """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertEquals(1, searchResult.results().getFirst().get("#version").asInt());
    assertEquals(0, searchResult.results().getFirst().get("#lifecycles").size());
  }

  @Test
  void queryAppraisalRuleEndDate() {

    String query =
        """
                   {
                     "$roots": [],
                     "$query": [
                     {
                        "$and": [
                          {"$lte": { "#management.AppraisalRule.EndDate": "%s" }},
                          {"$eq": { "#management.AppraisalRule.FinalAction": "Destroy" }}
                       ]
                     }
                     ],
                     "$filter": {},
                     "$projection": {"$fields": { "Title": 1 , "#management.AppraisalRule": 1 }}
                  }
                  """;

    String query1 = query.formatted(LocalDate.now());
    ResponseEntity<SearchResult<JsonNode>> r1 =
        restClient.searchArchive(tenant, acIdentifier, query1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    SearchResult<JsonNode> sr1 = r1.getBody();
    assertNotNull(sr1);
    assertEquals(20, sr1.results().size(), TestUtils.getBody(r1));

    String query2 = query.formatted(LocalDate.now().minusYears(3));
    var r2 = restClient.searchArchive(tenant, acIdentifier, query2);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> sr2 = r2.getBody();
    assertNotNull(sr2);
    assertEquals(10, sr2.results().size(), TestUtils.getBody(r2));
  }

  @Test
  void queryAppraisalRuleMaxEndDate() {

    String query =
        """
                       {
                         "$roots": [],
                         "$query": [
                         {
                            "$and": [
                              {"$lte": { "#computedInheritedRules.AppraisalRule.MaxEndDate": "%s" }},
                              {"$eq": { "#computedInheritedRules.AppraisalRule.FinalAction": "Destroy" }}
                           ]
                         }
                         ],
                         "$filter": {},
                         "$projection": {"$fields": { "Title": 1 , "#management.AppraisalRule": 1 }}
                      }
                      """;

    String query1 = query.formatted(LocalDate.now());
    ResponseEntity<SearchResult<JsonNode>> r1 =
        restClient.searchArchive(tenant, acIdentifier, query1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    SearchResult<JsonNode> sr1 = r1.getBody();
    assertNotNull(sr1);
    assertEquals(30, sr1.results().size(), TestUtils.getBody(r1));

    String query2 = query.formatted(LocalDate.now().minusYears(3));
    var r2 = restClient.searchArchive(tenant, acIdentifier, query2);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> sr2 = r2.getBody();
    assertNotNull(sr2);
    assertEquals(20, sr2.results().size(), TestUtils.getBody(r2));
  }

  @Test
  void queryStreamOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$exists": "#id"
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "#id": 1 }}
                }
                """;

    ResponseEntity<List<JsonNode>> response =
        restClient.searchArchiveStream(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    List<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(30, searchResult.size(), TestUtils.getBody(response));
  }

  @Test
  void queryStreamDepthOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$exists": "#id",
                       "$depth": 1
                     }
                   ],
                   "$filter": {},
                   "$projection": {}
                }
                """;

    ResponseEntity<List<JsonNode>> response =
        restClient.searchArchiveStream(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    List<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.size(), TestUtils.getBody(response));
  }

  @Test
  void queryMatchOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle1" },
                       "$depth": 1
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
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertEquals(1, searchResult.results().getFirst().get("#version").asInt());
    assertEquals(0, searchResult.results().getFirst().get("#lifecycles").size());
  }

  @Test
  void queryMatchOperatorFullSearchTest() {
    String query =
        """
           {
             "$roots": [],
             "$query": [
               {
                 "$match_all": { "full_search": "MyTitle1" },
                 "$depth": 1
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
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertEquals(1, searchResult.results().getFirst().get("#version").asInt());
    assertEquals(0, searchResult.results().getFirst().get("#lifecycles").size());
  }

  @Test
  void querySpecialProjectionTest() {
    String query =
        """
            {
              "$roots": [],
              "$query": [
                {
                  "$or": [
                    {
                      "$match_all": {
                        "full_search": "MyTitle1"
                      }
                    },
                    { "$in": { "#operations": ["t" ] } },
                    { "$in": { "#id": [ "t" ] }  }
                  ]
                }
              ],
              "$filter": {
                "$limit": 20,
                "$orderby": {
                  "StartDate": -1
                }
              },
              "$projection": {
                "$fields": {
                  "#id": 1,
                  "#allunitups": 1,
                  "#object": 1,
                  "#management": 1,
                  "#unitType": 1,
                  "#unitups": 1,
                  "#computedInheritedRules": 1,
                  "#validComputedInheritedRules": 1,
                  "DescriptionLevel": 1,
                  "OriginatingAgencyArchiveUnitIdentifier": 1,
                  "Title": 1,
                  "Description": 1,
                  "Type": 1,
                  "Domain": 1,
                  "Status": 1,
                  "PhysicalType": 1,
                  "PhysicalStatus": 1,
                  "PhysicalBarcode": 1,
                  "PhysicalAgency": 1,
                  "DocumentType": 1,
                  "CreationDate": 1,
                  "StartDate": 1,
                  "EndDate": 1,
                  "DigitalizationDate": 1,
                  "SendDate": 1,
                  "AdmissionDate": 1,
                  "RegistrationDate": 1,
                  "OperationDate": 1,
                  "DuaStartDate": 1,
                  "DuaEndDate": 1,
                  "Transmitter": 1
                }
              },
              "$facets": []
            }
            """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(22, searchResult.results().getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void queryRuleProjectionTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle1" },
                       "$depth": 1
                     }
                   ],
                   "$filter": {},
                   "$projection": { "$fields": { "#management.AppraisalRule.Rules": 1 , "#management.HoldRule.Rules": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);

    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(1, searchResult.results().getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void queryAgentProjectionTest() {
    String query =
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
                    "$projection": { "$fields": { "Directeur.Nom": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(1, searchResult.results().getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void queryRulesProjectionTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle1" },
                       "$depth": 1
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Directeur": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(0, searchResult.results().getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void queryRulesWithInheritedTest() {
    String query =
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
             "$projection": {"$fields": { "Title": 1, "Description": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
          }
          """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchiveWithInheritedRules(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();
    assertEquals(10, results.size(), TestUtils.getBody(response));

    JsonNode ir = results.getFirst().get("InheritedRules");
    assertNotNull(ir, TestUtils.getBody(response));

    int size = ir.get("AccessRule").get("Rules").size();
    assertEquals(0, size, TestUtils.getBody(response));

    String rule0 = ir.get("StorageRule").get("Rules").get(0).get("Rule").asText();
    assertEquals("STORAGERULE-000001", rule0, TestUtils.getBody(response));

    String rule1 = ir.get("StorageRule").get("Rules").get(1).get("Rule").asText();
    assertEquals("STORAGERULE-000002", rule1, TestUtils.getBody(response));

    String rule2 = ir.get("StorageRule").get("Rules").get(2).get("Rule").asText();
    assertEquals("STORAGERULE-000003", rule2, TestUtils.getBody(response));
  }

  @Test
  void queryComputedRulesWithInheritedTest() {
    String query =
        """
           {
             "$roots": [],
             "$query": [
               {
                 "$match": { "Title": "MyTitle2" },
                 "$depth": 2
               }
             ],
             "$filter": {},
             "$projection": {}
          }
          """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchiveWithInheritedRules(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();
    assertEquals(10, results.size(), TestUtils.getBody(response));

    JsonNode cir = results.getFirst().get("#computedInheritedRules");
    assertNotNull(cir, TestUtils.getBody(response));

    String appraisalDate = cir.get("AppraisalRule").get("MaxEndDate").asText();
    assertTrue(DateUtils.isLocalDate(appraisalDate), TestUtils.getBody(response));

    String appraisalOrigin = cir.get("AppraisalRule").get("InheritanceOrigin").asText();
    assertEquals("INHERITED", appraisalOrigin, TestUtils.getBody(response));

    String accessDate = cir.get("AccessRule").get("MaxEndDate").asText();
    assertTrue(DateUtils.isLocalDate(accessDate), TestUtils.getBody(response));

    String accessOrigin = cir.get("AccessRule").get("InheritanceOrigin").asText();
    assertEquals("LOCAL", accessOrigin, TestUtils.getBody(response));
  }

  @Test
  void queryBooleanTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        {
                          "$and": [
                            { "$match": { "Title": "MyTitle2" } },
                            {
                              "$not": [
                                { "$match": { "Title": "MyTitle3" } }
                              ]
                            }
                          ]
                        }
                      ],
                      "$depth": 2
                    }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();
    assertEquals(20, results.size(), TestUtils.getBody(response));
  }

  @Test
  void queryDepthTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        {
                          "$and": [
                            { "$match": { "Title": "MyTitle2" } },
                            {
                              "$not": [
                                { "$match": { "Title": "MyTitle3" } }
                              ]
                            }
                          ]
                        }
                      ],
                      "$depth": 1
                    }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                      """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(
        "MyTitle1",
        searchResult.results().getFirst().get("Title").asText(),
        TestUtils.getBody(response));
  }

  @Test
  void queryWithoutOperatorPrefixTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$and": { "Title": "MyTitle1" },
                       "$depth": 1
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage = "Failed to create operator 'Title' that does not start with $";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryNotOperatorWithoutOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        {
                          "$and": [
                            { "$match": { "Title": "MyTitle2" } },
                            {
                              "$not": { "Description": "Chapelle" }
                            }
                          ]
                        }
                      ]
                    }           ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage = "Failed to create operator 'Description' that does not start with $";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryWithRootsTest() {
    String query =
        """
                 {
                   "$roots": [%s],
                   "$query": [
                      {
                        "$or": [
                          { "$match": { "Title": "MyTitle1" } },
                          { "$match": { "Title": "MyTitle2" } },
                          { "$match": { "Title": "MyTitle3" } }
                        ]
                      }
                    ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """
            .formatted(unitIds.getFirst());

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(2, searchResult.results().size(), TestUtils.getBody(response));
    for (JsonNode unit : searchResult.results()) {
      assertTrue(
          unit.get("Title").asText().equals("MyTitle2")
              || unit.get("Title").asText().equals("MyTitle3"));
    }
  }

  @Test
  void queryMatchOperatorWithoutFieldTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        {
                          "$and": [
                            { "$match": { "Title": "MyTitle2" } },
                            {
                              "$not": { "$match": "MyTitle3" }
                            }
                          ]
                        }
                      ]
                    }           ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage = "Field is empty or does not exist";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryMatchWithoutDollarPrefixTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "match": { "Title": "MyTitle1" },
                       "$depth": 1
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage = "Failed to create operator 'match' that does not start with $";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryInOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                        "$and": [
                          {"$in": { "Version": [ "Version1", "Version2"] }} ,
                          {"$in": { "Directeur.Nom": [ "Deviller"], "$type": "DOCTYPE-000001" }},
                          {"$in": { "Directeur.Age": [ 78 ], "$type": "DOCTYPE-000001" }},
                          {"$in": { "Code": [ 94000 ], "$type": "DOCTYPE-000001" }}
                          ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1}}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(1, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryEmptyInOperatorTest() {

    String query =
        """
                     {
                       "$roots": [],
                       "$query": [
                         {
                            "$and": [
                              {"$in": { "Version": [] }} ,
                              {"$in": { "Directeur.Nom": [ "Deviller"], "$type": "DOCTYPE-000001" }},
                              {"$in": { "Directeur.Age": [ 78 ], "$type": "DOCTYPE-000001" }},
                              {"$in": { "Code": [ 94000 ], "$type": "DOCTYPE-000001" }}
                              ]
                         }
                       ],
                       "$filter": {},
                       "$projection": {"$fields": { "Title": 1}}
                    }
                    """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(0, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryInOperatorWithDocTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$in": { "Version": [ "Version1", "Version2" ] }} ,
                          {"$in": { "Directeur.Nom": [ "Deviller" ]}},
                          {"$in": { "Directeur.Age": [ 78 ]}},
                          {"$in": { "Code": [ 94000 ]}}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(1, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryInOperatorWithDocTypeEmptyProjectionTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$in": { "Version": [ "Version1", "Version2" ] }} ,
                          {"$in": { "Directeur.Nom": [ "Deviller" ]}},
                          {"$in": { "Directeur.Age": [ 78 ]}},
                          {"$in": { "Code": [ 94000 ]}}
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
    assertEquals(1, searchResult.results().size(), TestUtils.getBody(response));

    JsonNode cir = searchResult.results().getFirst().get("#computedInheritedRules");
    assertNotNull(cir, TestUtils.getBody(response));
  }

  @Test
  void queryInOperatorWithBadStringTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$in": { "Version": [ "Version1", 12] }
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage =
        "Field 'Version' with type 'keyword' and value '12' of type 'int' mismatch";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryInOperatorWithBadLongTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$in": { "#id": [ "12", 15] }
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.searchArchive(tenant, acIdentifier, query));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());

    String expectedMessage =
        "Field '#id' with type 'keyword' and value '15' of type 'int' mismatch";
    String actualMessage = thrown.getMessage();
    assertTrue(actualMessage.contains(expectedMessage), actualMessage);
  }

  @Test
  void queryInOperatorWithLongTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$in": { "#id": [ "1", "%s", "2", "%s"] }
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """
            .formatted(unitIds.getFirst(), unitIds.get(1));

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> nodes = searchResult.results();

    assertEquals(2, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(2, nodes.getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void queryInOperatorWithLongFullProjectionTest() {

    String query =
        """
           {
             "$roots": [],
             "$query": [
               {
                 "$in": { "#id": [ "1", "%s", "2", "%s"] }
               }
             ],
             "$filter": {},
             "$projection": {}
          }
          """
            .formatted(unitIds.getFirst(), unitIds.get(1));

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> nodes = searchResult.results();

    System.err.println(TestUtils.getBody(response));
    //    assertEquals(2, searchResult.results().size(), TestUtils.getBody(response));
    //    assertEquals(2, nodes.getFirst().size(), TestUtils.getBody(response));
  }

  @Test
  void querySortWithDocTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$in": { "Directeur.Nom": [ "Deviller" ]}},
                          {"$in": { "Code": [ 94000 ]}}
                       ]
                     }
                   ],
                   "$filter": {
                     "$offset": 2,
                     "$limit": 6,
                     "$orderby": { "Directeur.Age": 1 }
                   },
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<JsonNode> aus = searchResult.results();

    int age = -1;
    for (JsonNode au : aus) {
      int newAge = au.get("Directeur").get("Age").asInt();
      assertTrue(newAge > age, TestUtils.getBody(response));
      age = newAge;
    }

    int young = aus.getFirst().get("Directeur").get("Age").asInt();
    int old = aus.get(5).get("Directeur").get("Age").asInt();

    assertEquals(20, young, TestUtils.getBody(response));
    assertEquals(45, old, TestUtils.getBody(response));
  }

  @Test
  void queryTermsFacetsTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        { "$match": { "Title": "MyTitle2" } },
                        { "$match": { "Title": "MyTitle3" } }
                      ]
                    }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }},
                   "$facets": [
                      {
                        "$name": "Facet01",
                        "$terms": {
                           "$field": "#id",
                           "$size": 5
                         }
                      } ,
                      {
                        "$name": "Facet02",
                        "$terms": {
                           "$field": "DocumentType",
                           "$size": 5
                         }
                      },
                      {
                        "$name": "Facet03",
                        "$terms": {
                           "$field": "Directeur.Age",
                           "$size": 5,
                           "$order": "DESC"
                         }
                      }
                   ]
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    List<Facet> facets = response.getBody().facets();

    for (Facet facet : facets) {
      if ("Facet01".equals(facet.name())) {
        List<Bucket> buckets01 = facet.buckets();
        assertEquals(5, buckets01.size());
        for (Bucket bucket : buckets01) {
          assertEquals(1, bucket.count(), TestUtils.getBody(response));
          assertTrue(unitIds.contains(Long.parseLong(bucket.value())), TestUtils.getBody(response));
        }
      } else if ("Facet02".equals(facet.name())) {
        List<Bucket> buckets02 = facet.buckets();
        assertEquals(1, buckets02.size());
        Bucket bucket = buckets02.getFirst();
        assertEquals(20, bucket.count(), TestUtils.getBody(response));
        assertEquals("DOCTYPE-000001", bucket.value(), TestUtils.getBody(response));

      } else if ("Facet03".equals(facet.name())) {
        List<Bucket> buckets03 = facet.buckets();
        assertEquals(5, buckets03.size());
        for (Bucket bucket : buckets03) {
          assertEquals(1, bucket.count(), TestUtils.getBody(response));
          long age = Long.parseLong(bucket.value());
          assertTrue(age > 11 && age < 34, TestUtils.getBody(response));
        }
      }
    }
  }

  @Test
  void queryDateFacetsTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                    {
                      "$or": [
                        { "$match": { "Title": "MyTitle1" } },
                        { "$match": { "Title": "MyTitle2" } },
                        { "$match": { "Title": "MyTitle3" } }
                      ]
                    }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 , "#id":1}},
                   "$facets": [
                      {
                        "$name": "Facet01",
                        "$date_range": {
                             "$field": "CreatedDate",
                             "$format": "yyyy",
                             "$ranges": [
                                 { "$from": "2019", "$to": "2020" },
                                 { "$from": "2020", "$to": "2021" },
                                 { "$from": "2021", "$to": "2022" },
                                 { "$from": "2022", "$to": "2023" },
                                 { "$from": "2023", "$to": "2024" },
                                 { "$from": "2024" }
                             ]
                        }
                      }
                   ]
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);

    List<Facet> facets = response.getBody().facets();
    List<Bucket> buckets01 = facets.getFirst().buckets();
    assertEquals("Facet01", facets.getFirst().name(), TestUtils.getBody(response));
    assertEquals(6, buckets01.size(), TestUtils.getBody(response));

    for (Bucket bucket : buckets01) {
      switch (bucket.value()) {
        case "2019-2020", "2020-2021", "2024-2025" -> assertEquals(
            0, bucket.count(), TestUtils.getBody(response));
        case "2021-2022", "2022-2023", "2023-2024" -> assertEquals(
            10, bucket.count(), TestUtils.getBody(response));
      }
    }
  }

  @Test
  void queryFiltersFacetsTest() {
    String query =
        """
                {
                   "$roots":[ ],
                   "$type": "DOCTYPE-000001",
                   "$query":[
                      {
                         "$or":[
                            {
                               "$match":{
                                  "Title":"MyTitle1"
                               }
                            },
                            {
                               "$match":{
                                  "Title":"MyTitle2"
                               }
                            },
                            {
                               "$match":{
                                  "Title":"MyTitle3"
                               }
                            }
                         ]
                      }
                   ],
                   "$filter":{ },
                   "$projection":{
                      "$fields":{
                         "Title":1,
                         "Description":1
                      }
                   },
                   "$facets":[
                      {
                         "$name":"Facet01",
                         "$filters":{
                            "$query_filters":[
                               {
                                  "$name":"Titres1",
                                  "$query":{
                                     "$or":[
                                        {
                                           "$match":{
                                              "Title":"MyTitle1"
                                           }
                                        },
                                        {
                                           "$match":{
                                              "Title":"MyTitle2"
                                           }
                                        }
                                     ]
                                  }
                               },
                               {
                                  "$name":"Titres2",
                                  "$query":{
                                     "$or":[
                                        {
                                           "$match":{
                                              "Title":"MyTitle1"
                                           }
                                        },
                                        {
                                           "$match":{
                                              "Title":"MyTitle2"
                                           }
                                        },
                                        {
                                           "$match":{
                                              "Title":"MyTitle3"
                                           }
                                        }
                                     ]
                                  }
                               }
                            ]
                         }
                      },
                      {
                         "$name":"Facet02",
                         "$filters":{
                            "$query_filters":[
                               {
                                  "$name":"AgeNomCode",
                                  "$query":{
                                     "$and":[
                                        {
                                           "$in":{
                                              "Directeur.Nom":[
                                                 "Deviller"
                                              ]
                                           }
                                        },
                                        {
                                           "$in":{
                                              "Code":[
                                                 94000
                                              ]
                                           }
                                        }
                                     ]
                                  }
                               }
                            ]
                         }
                      }
                   ]
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);

    List<Facet> facets = response.getBody().facets();
    for (Facet facet : facets) {
      if ("Facet01".equals(facet.name())) {
        List<Bucket> buckets01 = facet.buckets();
        assertEquals(2, buckets01.size(), TestUtils.getBody(response));
        assertEquals(10, buckets01.getFirst().count(), TestUtils.getBody(response));
        assertEquals("Titres1", buckets01.getFirst().value(), TestUtils.getBody(response));
        assertEquals(20, buckets01.get(1).count(), TestUtils.getBody(response));
        assertEquals("Titres2", buckets01.get(1).value(), TestUtils.getBody(response));

      } else if ("Facet02".equals(facet.name())) {
        List<Bucket> buckets02 = facet.buckets();
        assertEquals(1, buckets02.size(), TestUtils.getBody(response));
        assertEquals(10, buckets02.getFirst().count(), TestUtils.getBody(response));
      }
    }
  }

  @Test
  void queryFiltersFacetsTest2() {
    String query =
        """
                {
                  "$roots":[ ],
                  "$query":[ { "$exists":"#id" } ],
                  "$filter": { "$limit":1  },
                  "$projection": { "$fields":{ "Title":1, "Description":1 } },
                  "$facets":[
                    {
                      "$name":"collections",
                      "$filters":{
                        "$query_filters":[
                          {
                            "$name":"",
                            "$query":{
                              "$in":{ "#allunitups":[ "" ] }
                            }
                          },
                          {
                            "$name":"root",
                            "$query":{ "$exists":"#id" }
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);

    List<Facet> facets = response.getBody().facets();

    List<Bucket> buckets01 = facets.getFirst().buckets();
    assertEquals("collections", facets.getFirst().name(), TestUtils.getBody(response));
    assertEquals(2, buckets01.size(), TestUtils.getBody(response));
    assertEquals(0, buckets01.getFirst().count(), TestUtils.getBody(response));
  }

  @Test
  void queryFiltersFacetsTest3() {
    String query =
        """
            {
              "$roots": [],
              "$query": [
                {
                  "$and": [
                    {
                      "$or": [
                        {
                          "$match_all": {
                            "full_search": "MyTitle1"
                          }
                        },
                        {
                          "$in": {
                            "#operations": [
                              "MyTitle2"
                            ]
                          }
                        },
                        {
                          "$in": {
                            "#id": [
                              "MyTitle2"
                            ]
                          }
                        }
                      ]
                    },
                    {
                      "$or": [
                        {
                          "$range": {
                            "#management.AppraisalRule.Rules.StartDate": {
                              "$gte": "2000-01-01",
                              "$lte": "2024-12-31T22:59:59.999Z"
                            }
                          }
                        },
                        {"$gt":{"#management.HoldRule.Rules.EndDate":"2024-03-25"}},
                        {"$and":[
                          {"$not":[{"$exists":"#management.HoldRule.Rules"}]},
                          {"$not":[{"$exists":"#management.HoldRule.Rules.EndDate"}]}]}
                      ]
                    }
                  ]
                }
              ],
              "$filter": {
                "$limit": 1,
                "$orderby": {}
              },
              "$projection": {
                "$fields": {
                  "#id": 1
                }
              },
              "$facets": [
                {
                  "$name": "duaStartDate",
                  "$date_range": {
                    "$field": "#management.AppraisalRule.Rules.StartDate",
                    "$format": "yyyy",
                    "$ranges": [
                      { "$to": "2020" },
                      { "$from": "2020", "$to": "2021" },
                      { "$from": "2021", "$to": "2022" },
                      { "$from": "2022", "$to": "2023" },
                      { "$from": "2023", "$to": "2024" },
                      { "$from": "2024" }
                    ]
                  }
                }
              ]
            }
            """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);

    List<Facet> facets = response.getBody().facets();
    List<Bucket> buckets01 = facets.getFirst().buckets();
    assertEquals("duaStartDate", facets.getFirst().name(), TestUtils.getBody(response));
    assertEquals(6, buckets01.size(), TestUtils.getBody(response));
    assertEquals(10, buckets01.getFirst().count(), TestUtils.getBody(response));

    for (Bucket bucket : buckets01) {
      switch (bucket.value()) {
        case "2000-2020" -> assertEquals(10, bucket.count(), TestUtils.getBody(response));
        case "2020-2021", "2024-2099", "2021-2022", "2022-2023", "2023-2024" -> assertEquals(
            0, bucket.count(), TestUtils.getBody(response));
      }
    }
  }

  @Test
  void queryMatchAllOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match_all": { "Description": "My second Description of Archive Unit" },
                       "$depth": 2
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Description": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
    assertEquals(
        "My Description of second Archive Unit",
        searchResult.results().getFirst().get("Description").asText());
  }

  @Test
  void queryMatchPhraseOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match_phrase": { "Description": "My Description of" }
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
    assertEquals(20, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryMatchPhrasePrefixOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match_phrase_prefix": { "Description": "My Descrip" }
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
    assertEquals(20, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryMatchPhrasePrefixOperatorOrderByTitleTest() {
    String query =
        """
                     {
                       "$roots": [],
                       "$query": [
                         {
                           "$match_phrase_prefix": { "Title": "MyTitle" }
                         }
                       ],
                       "$filter": {
                         "$limit": 100,
                         "$orderby": { "Title.keyword": -1 }
                       },
                       "$projection": { "$fields": { "Title": 1 }}
                    }
                    """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(30, searchResult.results().size(), TestUtils.getBody(response));

    String t0 = searchResult.results().getFirst().get("Title").asText();
    for (int i = 1; i < 30; i++) {
      String t1 = searchResult.results().get(i).get("Title").asText();
      assertTrue(t0.compareTo(t1) >= 0, TestUtils.getBody(response));
      t0 = t1;
    }
  }

  @Test
  void queryWildcardPrefixOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$wildcard": { "Version": "Vers*2" }
                     }
                   ],
                   "$filter": {
                     "$limit": 15,
                     "$orderby": { "DocumentType_keyword": 1 }
                   },
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryRegexOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$regex": { "Version": "Ver..on[1,3]" }
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
    assertEquals(20, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryEqAndNeqOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$neq": { "Directeur.Age": 78 }},
                          {"$eq": { "Code": 94000 }}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(9, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryGteOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$gte": { "Directeur.Age": 40 }}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(4, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryGtOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$gt": { "Directeur.Age": 40 }}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(3, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryLteOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$lte": { "Directeur.Age": 40 }}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(7, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryLtOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$lt": { "Directeur.Age": 40 }}
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(6, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryNinOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$nin": { "Directeur.Age": [40, 45, 133] }}
                        ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(8, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void querySearchOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$search": { "Version": "Version2" }} ,
                          {"$search": { "Directeur.Nom": "Deviller" }},
                          {"$search": { "Directeur.Age": "40 45 78 176" }},
                          {"$search": { "Directeur.Prenom": "Emman*" }}
                        ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(3, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryExistsOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$exists": "Title"}
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
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryExistsOperatorWithDocTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }},
                          {"$exists": { "Directeur.Age" : true, "$type": "DOCTYPE-000001"}}
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
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryNotExistsOperatorWithDocTypeTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }},
                          {"$exists": { "Directeur.Prenom" : false, "$type": "DOCTYPE-000001"}}
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
    assertEquals(0, searchResult.results().size(), TestUtils.getBody(response));
  }

  @Test
  void queryRangeOperatorTest() {

    String query =
        """
                 {
                   "$roots": [],
                   "$type": "DOCTYPE-000001",
                   "$query": [
                     {
                        "$and": [
                          {"$eq": { "Version": "Version2" }} ,
                          {"$eq": { "Directeur.Nom": "Deviller" }},
                          {"$range": { "Directeur.Age": { "$gt": 20 ,  "$lte" : 67 } } }
                       ]
                     }
                   ],
                   "$filter": {},
                   "$projection": {"$fields": { "Title": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(6, searchResult.results().size(), TestUtils.getBody(response));
  }
}
