/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.common.utils.JsonUtils.toLongs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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

class ArchiveReclassifyIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private UserDto userDto;

  @BeforeAll
  void beforeAll() throws IOException, ParsingException {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void reclassificationTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[10];
    Arrays.fill(sips, 0, sips.length, sip);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sips);
    Utils.sleep(1000);

    List<Long> id1s = ids.stream().map(m -> m.get("UNIT_ID1")).toList();
    List<Long> id2s = ids.stream().map(m -> m.get("UNIT_ID2")).toList();
    List<Long> id3s = ids.stream().map(m -> m.get("UNIT_ID3")).toList();

    String searchQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "%s" }
                     }
                   ],
                   "$filter": {},
                   "$projection": {}
                }
                """;

    reclassify1(tenant, id1s, id2s, id3s, searchQuery);
    reclassify2(tenant, id2s, id3s);
    reclassify3(tenant, id1s, id2s, id3s, searchQuery);
  }

  private void reclassify1(
      Long tenant,
      List<Long> title1Ids,
      List<Long> title2Ids,
      List<Long> title3Ids,
      String searchQuery) {

    // Title1 / Title2 / title3
    // =>
    // Title1 / Title2[0] / title3
    // Title1 / Title2[1-]

    String reclassificationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle3" }
                     }
                   ],
                   "$filter": {},
                   "$action": [
                        { "$add": { "#unitup": "%s" }
                        }
                    ]
                }
                """
            .formatted(title2Ids.getFirst());

    ResponseEntity<String> r2 =
        restClient.reclassifyArchive(tenant, acIdentifier, reclassificationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    String searchQuery3 = searchQuery.formatted("MyTitle3");
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery3);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> reclassifiedUnits = r3.getBody().results();

    assertEquals(title3Ids.size(), reclassifiedUnits.size(), TestUtils.getBody(r3));
    for (JsonNode reclassifiedUnit : reclassifiedUnits) {
      assertEquals("MyTitle3", reclassifiedUnit.get("Title").asText());
      assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
      assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
      assertEquals(title1Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
    }
  }

  private void reclassify2(Long tenant, List<Long> title2Ids, List<Long> title3Ids) {

    // Title1 / Title2[0] / title3
    // Title1 / Title2[1-]
    // =>
    // Title1 / Title2[0] / title3[0] / title2[1] / title3[1] / title2[2] / title3[2]
    // Title1 / Title2[0] / Title3[3-]
    // Title1 / Title2[3-]

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

    List<String> rcs = new ArrayList<>();
    rcs.add(reclassificationQuery.formatted(title2Ids.get(1), title3Ids.getFirst()));
    rcs.add(reclassificationQuery.formatted(title3Ids.get(1), title2Ids.get(1)));
    rcs.add(reclassificationQuery.formatted(title2Ids.get(2), title3Ids.get(1)));
    rcs.add(reclassificationQuery.formatted(title3Ids.get(2), title2Ids.get(2)));

    for (String rc : rcs) {
      ResponseEntity<String> r2 = restClient.reclassifyArchive(tenant, acIdentifier, rc);
      assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
      String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));
    }

    String searchQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$in": { "#id": [ "%s", "%s", "%s", "%s", "%s", "%s"] }
                     }
                   ],
                   "$filter": {},
                   "$projection": {}
                }
                """
            .formatted(
                title2Ids.getFirst(),
                title3Ids.getFirst(),
                title2Ids.get(1),
                title3Ids.get(1),
                title2Ids.get(2),
                title3Ids.get(2));

    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> reclassifiedUnits = r3.getBody().results();

    for (JsonNode reclassifiedUnit : reclassifiedUnits) {
      Long id = reclassifiedUnit.get("#id").asLong();
      if (id.equals(title3Ids.get(2))) {
        assertEquals(title2Ids.get(2), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.get(2), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title3Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).get(2));
        assertEquals(title3Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(3));
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(4));
        assertEquals(6, toLongs(reclassifiedUnit.get("#allunitups")).size());
      } else if (id.equals(title2Ids.get(2))) {
        assertEquals(title3Ids.get(1), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title3Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
        assertEquals(title3Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(2));
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(3));
        assertEquals(5, toLongs(reclassifiedUnit.get("#allunitups")).size());
      } else if (id.equals(title3Ids.get(1))) {
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title3Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(2));
        assertEquals(4, toLongs(reclassifiedUnit.get("#allunitups")).size());
      } else if (id.equals(title2Ids.get(1))) {
        assertEquals(title3Ids.getFirst(), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title3Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
        assertEquals(3, toLongs(reclassifiedUnit.get("#allunitups")).size());
      } else if (id.equals(title3Ids.getFirst())) {
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(2, toLongs(reclassifiedUnit.get("#allunitups")).size());
      } else if (id.equals(title2Ids.getFirst())) {
        //        assertEquals(2, toLongs(reclassifiedUnit.get("#allunitups")).size());
        assertEquals(1, toLongs(reclassifiedUnit.get("#allunitups")).size());
      }
    }
  }

  private void reclassify3(
      Long tenant,
      List<Long> title1Ids,
      List<Long> title2Ids,
      List<Long> title3Ids,
      String searchQuery) {

    // Title1 / Title2[0] / title3[0] / title2[1] / title3[1] / title2[2] / title3[2]
    // Title1 / Title2[0] / Title3[3-]
    // Title1 / Title2[3-]
    // =>
    // Title1[7] / Title2[0] / title3[0]
    // Title1[7] / Title2[0] / Title3[3-]
    // Title1[7] / title2[1] / title3[1]
    // Title1[7] / title2[2] / title3[2]
    // Title1[7] / Title2[3-]

    String reclassificationQuery =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$match": { "Title": "MyTitle2" }
                     }
                   ],
                   "$filter": {},
                   "$action": [
                        { "$add": { "#unitup": "%s" }
                        }
                    ]
                }
                """
            .formatted(title1Ids.get(7));

    ResponseEntity<String> r2 =
        restClient.reclassifyArchive(tenant, acIdentifier, reclassificationQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    // Wait for Indexation
    String searchQuery3 = searchQuery.formatted("MyTitle3");
    ResponseEntity<SearchResult<JsonNode>> r3 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery3);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(r3.getBody());
    List<JsonNode> reclassifiedUnits = r3.getBody().results();
    for (JsonNode reclassifiedUnit : reclassifiedUnits) {
      assertEquals("MyTitle3", reclassifiedUnit.get("Title").asText());
      assertEquals(2, toLongs(reclassifiedUnit.get("#allunitups")).size());

      Long id = reclassifiedUnit.get("#id").asLong();
      if (id.equals(title3Ids.getFirst())) {
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#allunitups")).get(1));

      } else if (id.equals(title3Ids.get(1))) {
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.get(1), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#allunitups")).get(1));

      } else if (id.equals(title3Ids.get(2))) {
        assertEquals(title2Ids.get(2), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.get(2), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#allunitups")).get(1));

      } else {
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
        assertEquals(title2Ids.getFirst(), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
        assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#allunitups")).get(1));
      }
    }

    String searchQuery2 = searchQuery.formatted("MyTitle2");
    ResponseEntity<SearchResult<JsonNode>> r4 =
        restClient.searchArchive(tenant, acIdentifier, searchQuery2);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    assertNotNull(r4.getBody());
    List<JsonNode> reclassifiedUnits4 = r4.getBody().results();
    for (JsonNode reclassifiedUnit : reclassifiedUnits4) {
      assertEquals("MyTitle2", reclassifiedUnit.get("Title").asText());
      assertEquals(1, toLongs(reclassifiedUnit.get("#allunitups")).size());
      assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#unitups")).getFirst());
      assertEquals(title1Ids.get(7), toLongs(reclassifiedUnit.get("#allunitups")).getFirst());
    }
  }
}
