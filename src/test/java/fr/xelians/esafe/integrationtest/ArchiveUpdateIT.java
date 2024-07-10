/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.common.utils.Utils;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.XPathContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Execution(ExecutionMode.SAME_THREAD)
class ArchiveUpdateIT extends BaseIT {

  private final List<String> requestIds = new ArrayList<>();
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
    }

    Utils.sleep(1000);
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void updateManagementTest() {
    String updateQuery =
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
                   "$action": [
                        { "$set": { "#management.AppraisalRule.Rules":
                                     [
                                        {
                                          "Rule": "APPRAISALRULE-000004",
                                          "StartDate": "1969-02-27"
                                        }
                                     ],
                                     "Description" : "Patched Description v666"
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

    String searchQuery =
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

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);

    List<JsonNode> results = searchResult.results();
    JsonNode first = results.getFirst();
    assertEquals(10, results.size(), TestUtils.getBody(r2));
    assertEquals("MyTitle1", first.get("Title").asText());
    assertEquals("Patched Description v666", first.get("Description").asText());
    assertEquals(
        "APPRAISALRULE-000004",
        first.get("#management").get("AppraisalRule").get("Rules").get(0).get("Rule").asText(),
        TestUtils.getBody(r2));
  }

  @Test
  void updateEmptyManagementTest() {
    String updateQuery =
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
                   "$action": [
                        { "$set": { "#management" : {"AppraisalRule": { "Rules": [] }}}}
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, ope.status(), TestUtils.getBody(r1));

    String searchQuery =
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

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);
    assertEquals(10, searchResult.results().size(), TestUtils.getBody(r2));
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertNull(searchResult.results().getFirst().get("#management").get("AppraisalRule"));
  }

  @Test
  void updateBadFieldTest() {
    String updateQuery =
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
                   "$action": [
                        { "$set": { "#management" : {"Toto": { "Rules": [] }}}}
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), ope.message());
    assertTrue(ope.message().contains("Failed to patch"));
  }

  @Test
  void updateExtFieldTest() {
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

    String searchQuery =
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
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();

    assertEquals(10, results.size(), TestUtils.getBody(r2));
    assertEquals("MyTitle2", results.getFirst().get("Title").asText());
    assertEquals(98766, results.getFirst().get("Code").asLong());
    assertEquals("Martin", results.getFirst().get("Directeur").get("Nom").asText());
    assertEquals(799, results.getFirst().get("Directeur").get("Age").asLong());
    assertEquals("Robert", results.getFirst().get("Directeur").get("Prenom").asText());
  }

  @Test
  void updateExtArrayFieldTest() {
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
                        { "$set": { "Directeur": [
                                        {
                                          "Age": 799,
                                          "Nom": "Martin",
                                          "Prenom": "Robert"
                                        },
                                        {
                                          "Age": 379,
                                          "Nom": "Dupond",
                                          "Prenom": "Jean"
                                        }
                                     ]
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

    String searchQuery =
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
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();

    assertEquals(10, results.size(), TestUtils.getBody(r2));
    assertEquals("MyTitle2", results.getFirst().get("Title").asText());
    assertEquals(98766, results.getFirst().get("Code").asLong());
    assertEquals("Martin", results.getFirst().get("Directeur").get(0).get("Nom").asText());
    assertEquals(799, results.getFirst().get("Directeur").get(0).get("Age").asLong());
    assertEquals("Jean", results.getFirst().get("Directeur").get(1).get("Prenom").asText());
  }

  @Test
  void updateLfcTest() {

    String searchQuery =
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
                       "$orderby": { "#id": 1 }
                   },
                   "$projection": {}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r1 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));
    assertNotNull(r1.getBody());
    List<JsonNode> units = r1.getBody().results();

    String updateQuery =
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
                   "$action": [
                        { "$set": { "Description": "The new description" }}
                    ]
                }
                """;

    ResponseEntity<String> r2 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto ope = restClient.waitForOperation(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, ope.status(), TestUtils.getBody(r2));

    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> patchedUnits = r3.getBody().results();

    for (int i = 0; i < patchedUnits.size(); i++) {
      JsonNode unit = units.get(i);
      JsonNode patchedUnit = patchedUnits.get(i);

      List<String> opIds = JsonUtils.toStrings(unit.get("#operations"));
      opIds.add(ope.id());

      assertEquals(opIds, JsonUtils.toStrings(patchedUnit.get("#operations")));
      assertEquals(unit.get("#id").asLong(), patchedUnit.get("#id").asLong());
      assertEquals(unit.get("#version").asInt() + 1, patchedUnit.get("#version").asInt());
      assertEquals(unit.get("#lifecycles").size() + 1, patchedUnit.get("#lifecycles").size());
      assertEquals(1, patchedUnit.get("#lifecycles").get(0).get("#lfc_version").asInt());
    }
  }

  @Test
  void updateDescriptionArrayFieldTest() {
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
                        { "$set": { "Description": [ "Coucou" ]}}
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), TestUtils.getBody(r1));
  }

  @Test
  void updateDocumentTypeTest() {
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
                        { "$set": { "DocumentType": "" }}
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), TestUtils.getBody(r1));
  }

  @Test
  void removeDocumentTypeTest() {
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
                        { "$unset": { "DocumentType": "" }}
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), TestUtils.getBody(r1));
  }

  @Test
  void removeExtArrayFieldTest() {
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
                        { "$set": { "Code": 90887 }},
                        { "$unset": [ "Directeur" ] }
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, ope.status(), TestUtils.getBody(r1));

    String searchQuery =
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

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();

    assertEquals(10, results.size(), TestUtils.getBody(r2));
    assertEquals("MyTitle2", results.getFirst().get("Title").asText());
    assertEquals(90887, results.getFirst().get("Code").asLong());
    assertNull(results.getFirst().get("Directeur"));
  }

  @Test
  void updateTitleTest() {
    String updateQuery =
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
                           "$action": [
                              { "$set": { "Title": "" }}
                            ]
                        }
                        """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), TestUtils.getBody(r1));
    assertTrue(ope.message().contains("'Title' cannot be empty"), TestUtils.getBody(r1));
  }

  @Test
  void removeTitleTest() {
    String updateQuery =
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
                       "$action": [
                            { "$unset": [ "Title"] }
                        ]
                    }
                    """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, ope.status(), TestUtils.getBody(r1));
    assertTrue(ope.message().contains("'Title' cannot be unset"));
  }

  @Test
  void removeManagementTest() {
    String updateQuery =
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
                   "$action": [
                        { "$unset": [ "#management.AppraisalRule.Rules", "Description" ] }
                    ]
                }
                """;

    ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto ope =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, ope.status(), TestUtils.getBody(r1));

    String searchQuery =
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
                   "$projection": { "$fields": {"Title": 1, "Description": 1, "#management": 1 }  }
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> r2 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    SearchResult<JsonNode> searchResult = r2.getBody();
    assertNotNull(searchResult);
    List<JsonNode> results = searchResult.results();
    assertEquals(10, results.size(), TestUtils.getBody(r2));
    assertEquals("MyTitle1", results.getFirst().get("Title").asText(), TestUtils.getBody(r2));
    assertNull(results.get(2).get("Description"), TestUtils.getBody(r2));
    assertTrue(
        results.get(2).get("#management").get("AppraisalRule").get("Rules").isEmpty(),
        TestUtils.getBody(r2));
  }

  @Test
  void updateMultipleTest() {
    String updateQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle1" },
                       "$depth": 2
                     }
                   ],
                   "$filter": {},
                   "$action": [
                        { "$set": { "Description": "PLACE_HOLDER" }}
                    ]
                }
                """;

    List<String> rs = Collections.synchronizedList(new ArrayList<>());

    IntStream.rangeClosed(1, 4)
        .parallel()
        .forEach(
            i -> {
              String query = updateQuery.replace("PLACE_HOLDER", "description_" + i);
              ResponseEntity<String> r1 = restClient.updateArchive(tenant, acIdentifier, query);
              assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));
              String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
              rs.add(requestId);
            });

    for (String r : rs) {
      OperationStatusDto ope =
          restClient.waitForOperationStatus(tenant, r, 30, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, ope.status(), r);
    }
  }
}
