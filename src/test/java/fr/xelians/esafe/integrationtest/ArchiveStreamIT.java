/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.model.ArchiveUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

class ArchiveStreamIT extends BaseIT {

  private String requestId;
  private ArchiveTransfer simpleSip;
  private String acIdentifier;

  private Long tenant;

  @BeforeAll
  void beforeAll(@TempDir Path tmpDir) throws IOException {
    SetupDto setupDto = setup();
    UserDto userDto = setupDto.userDto();
    tenant = setupDto.tenant();

    Scenario.createScenario02(restClient, tenant, userDto);

    int identifier = 1;
    acIdentifier = "AC-" + TestUtils.pad(identifier);

    // Upload SIP 1
    Path sipPath1 = tmpDir.resolve("largesip1.zip");
    simpleSip = SipFactory.createLargeSip(tmpDir, identifier, 3267);
    sedaService.write(simpleSip, sipPath1);
    ResponseEntity<Void> response1 = restClient.uploadSip(tenant, sipPath1);
    assertEquals(HttpStatus.ACCEPTED, response1.getStatusCode(), TestUtils.getBody(response1));

    // Upload SIP
    Path sipPath2 = tmpDir.resolve("largesip2.zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, identifier, 3040), sipPath2);
    ResponseEntity<Void> response2 = restClient.uploadSip(tenant, sipPath2);
    assertEquals(HttpStatus.ACCEPTED, response2.getStatusCode(), TestUtils.getBody(response2));

    // Upload SIP 1
    Path sipPath3 = tmpDir.resolve("largesip3.zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, identifier, 2960), sipPath3);
    ResponseEntity<Void> response3 = restClient.uploadSip(tenant, sipPath3);
    assertEquals(HttpStatus.ACCEPTED, response3.getStatusCode(), TestUtils.getBody(response3));

    // Wait SIP to be loaded and indexed
    requestId = response1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 120, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(response1));

    OperationStatusDto operation2 =
        restClient.waitForOperationStatus(
            tenant, response2.getHeaders().getFirst(X_REQUEST_ID), 120, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation2.status(), TestUtils.getBody(response2));

    OperationStatusDto operation3 =
        restClient.waitForOperationStatus(
            tenant, response3.getHeaders().getFirst(X_REQUEST_ID), 120, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation3.status(), TestUtils.getBody(response3));

    // Wait 1 sec to let lucene commit the indexed unit
    Utils.sleep(1000);
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void downloadBinary(@TempDir Path tmpDir) throws IOException, ParsingException {

    // Download ATR
    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    // Retrieve all created archive units from ATR
    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

    //    int[] idx = {0, 100, 477, 1000, 2578, 5000, 6266};
    int[] idx = {0, 100, 477, 1000, 2578};

    for (int i : idx) {
      ArchiveUnit unit = simpleSip.getArchiveUnits().get(i);
      // Get archive unit id (SystemId) from archive unit id attribute
      String query = "//ns:ArchiveUnit[@id='" + unit.getId() + "']";
      Nodes nodes = rootElem.query(query, xc);
      String unitId = nodes.get(0).getValue();
      if (unit.getBinaryPath() != null) {
        // Download binary object from Archive Unit
        Path binPath =
            restClient.getBinaryObjectByUnitId(
                tenant, tmpDir, acIdentifier, unitId, unit.getBinaryVersion());

        assertNotNull(binPath);
        assertTrue(Files.exists(binPath));
        assertEquals(Files.size(unit.getBinaryPath()), Files.size(binPath));
        assertArrayEquals(
            HashUtils.checksum(Hash.SHA512, unit.getBinaryPath()),
            HashUtils.checksum(Hash.SHA512, binPath));
      }
    }
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
    // assertEquals(12267, searchResult.size(), TestUtils.getBody(response));
    assertEquals(9267, searchResult.size(), TestUtils.getBody(response));
  }

  @Test
  void queryOperatorTest() {
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [
                     {
                       "$exists": "#id"
                     }
                   ],
                   "$filter": {
                     "$offset": 0,
                     "$limit": 20000,
                     "$orderby": { "#id": 1 }
                   },
                   "$projection": {"$fields": { "#id": 1 }}
                }
                """;

    ResponseEntity<SearchResult<JsonNode>> response =
        restClient.searchArchive(tenant, acIdentifier, query);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    SearchResult<JsonNode> searchResult = response.getBody();
    assertNotNull(searchResult);
    assertEquals(9267, searchResult.hits().size(), TestUtils.getBody(response));
  }
}
