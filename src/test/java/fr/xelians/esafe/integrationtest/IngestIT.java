/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.common.utils.JsonUtils.toLongs;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.atr.ArchiveUnitReply;
import fr.xelians.esafe.archive.domain.atr.BinaryDataObjectReply;
import fr.xelians.esafe.archive.domain.ingest.sedav2.Sedav2Utils;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationResult;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.TenantContract;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.dto.AccessContractDto;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.esafe.referential.dto.ProfileDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.model.ArchiveUnit;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import nu.xom.*;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class IngestIT extends BaseIT {

  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void createCsvAgenciesTest() throws IOException {
    Path dir = Paths.get(ItInit.AGENCY);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".csv")) {
      Long tenant = nextTenant();
      ResponseEntity<List<AgencyDto>> response = restClient.createCsvAgency(tenant, path);
      assertEquals(
          HttpStatus.CREATED,
          response.getStatusCode(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(response)));
    }
  }

  @Test
  void ingestHoldingTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    String[] agencies = {
      "FRAN_NP_009915", "Identifier4", "Identifier5", "Service_versant", "Service_producteur"
    };
    for (var agency : agencies) {
      var r0 = restClient.createAgencies(tenant, DtoFactory.createAgencyDto(agency));
      assertEquals(HttpStatus.CREATED, r0.getStatusCode(), TestUtils.getBody(r0));
    }

    Path dir = Paths.get(ItInit.SEDA_HOLDING);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_arbre_", ".zip")) {
      ResponseEntity<Void> r3 = restClient.uploadHolding(tenant, path);
      String requestId = r3.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

      assertEquals(
          OperationStatus.OK,
          operation.status(),
          String.format("path: %s - r2: %s", path, TestUtils.getBody(r3)));
      assertEquals(
          HttpStatus.ACCEPTED,
          r3.getStatusCode(),
          String.format("path: %s - r2: %s", path, TestUtils.getBody(r3)));
    }

    // Get unit and assert parent unit == systemId
    String query =
        """
               {
                 "$roots": [],
                 "$query": [ { "$eq": { "#unitType": "HOLDING_UNIT" } } ],
                 "$filter": {},
                 "$projection": {}
              }
              """;

    String acIdentifier = "AC-" + TestUtils.pad(1);
    SearchResult<JsonNode> searchResult = searchArchives(tenant, acIdentifier, query, 12);
    assertNotNull(searchResult, "Search Time out");

    List<JsonNode> units = searchResult.results();
    assertEquals(12, units.size(), searchResult.toString());
  }

  @Test
  void ingestHoldingKoTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path dir = Paths.get(ItInit.SEDA_HOLDING);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_arbre_", ".zip")) {
      ResponseEntity<Void> response = restClient.uploadHolding(tenant, path);
      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, OperationStatus.ERROR_CHECK);

      assertEquals(OperationStatus.ERROR_CHECK, operation.status());
      assertEquals(
          HttpStatus.ACCEPTED,
          response.getStatusCode(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(response)));
    }
  }

  @Test
  void ingestFilingTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    String[] agencies = {"Identifier4", "Service_versant", "Service_producteur", "Identifier5"};
    for (var agency : agencies) {
      var r0 = restClient.createAgencies(tenant, DtoFactory.createAgencyDto(agency));
      assertEquals(HttpStatus.CREATED, r0.getStatusCode(), TestUtils.getBody(r0));
    }

    ResponseEntity<List<IngestContractDto>> r3 =
        restClient.createIngestContract(tenant, DtoFactory.createIngestContractDto("IC-000004"));
    assertEquals(HttpStatus.CREATED, r3.getStatusCode(), TestUtils.getBody(r3));

    Path dir = Paths.get(ItInit.SEDA_FILING);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_plan_", ".zip")) {
      ResponseEntity<Void> r4 = restClient.uploadFiling(tenant, path);
      String requestId = r4.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

      assertEquals(
          OperationStatus.OK,
          operation.status(),
          String.format("path: %s - r0: %s", path, TestUtils.getBody(r4)));
      assertEquals(
          HttpStatus.ACCEPTED,
          r4.getStatusCode(),
          String.format("path: %s - r0: %s", path, TestUtils.getBody(r4)));
    }
  }

  @Test
  void ingestFilingKoTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path dir = Paths.get(ItInit.SEDA_FILING);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO", ".zip")) {
      ResponseEntity<Void> response = restClient.uploadFiling(tenant, path);
      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, OperationStatus.ERROR_CHECK);

      assertEquals(OperationStatus.ERROR_CHECK, operation.status());
      assertEquals(
          HttpStatus.ACCEPTED,
          response.getStatusCode(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(response)));
    }
  }

  @Test
  void downloadATR(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);
    assertNotNull(atrPath);
    assertTrue(Files.exists(atrPath));
    assertTrue(Files.size(atrPath) > 1000);
  }

  @Test
  void downloadManifest(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 1), sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    Path mftPath = tmpDir.resolve(requestId + ".mft");
    restClient.downloadManifest(tenant, requestId, mftPath);

    assertNotNull(mftPath);
    assertTrue(Files.exists(mftPath));
    assertTrue(Files.size(mftPath) > 1000);
  }

  @Test
  void downloadManifestFailed(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    IngestContractDto ingestContractDto = DtoFactory.createIngestContractDto(1);
    ingestContractDto.setStoreManifest(false);
    ResponseEntity<IngestContractDto> r1 =
        restClient.updateIngestContract(tenant, ingestContractDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ArchiveTransfer sip = SipFactory.createSimpleSip(tmpDir, 1);
    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(sip, sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    Path mftPath = tmpDir.resolve(requestId + ".mft");

    // Check manifest does not exist
    HttpClientErrorException t1 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.downloadManifest(tenant, requestId, mftPath));
    assertEquals(HttpStatus.NOT_FOUND, t1.getStatusCode(), t1.toString());
  }

  @Test
  void downloadAtrFromBadSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createSimpleSip(tmpDir, 1);
    sip.setArchivalAgreement("Bad_ArchivalAgreement");
    sedaService.write(sip, sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.ERROR_CHECK, operation.status());

    // Download XML ATR
    Path atrPath = tmpDir.resolve(requestId + ".atr");
    HttpClientErrorException t1 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.downloadXmlAtr(tenant, requestId, atrPath));
    assertEquals(HttpStatus.NOT_FOUND, t1.getStatusCode(), t1.toString());
  }

  @Test
  void downloadJsonAtr(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    int identifier = 1;
    String acIdentifier = "AC-" + TestUtils.pad(identifier);

    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer smallSip = SipFactory.createSmallSip(tmpDir, 1);
    sedaService.write(smallSip, sipPath);
    ResponseEntity<Void> r1 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r1.getStatusCode(), TestUtils.getBody(r1));

    String requestId = r1.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    // Download Json ATR
    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadJsonAtr(tenant, requestId, atrPath);

    ArchiveTransferReply atr = JsonService.toArchiveTransferReply(atrPath);
    Map<String, ArchiveUnitReply> unitMap = atr.getArchiveUnitReplyMap();

    // Check archive units from Json ATR
    for (ArchiveUnit unit : smallSip.getArchiveUnits()) {
      // Get archive unit id (SystemId) from archive unit id attribute
      String unitId = unitMap.get(unit.getId()).getSystemId();
      if (unit.getBinaryPath() != null) {
        // Download binary object from Archive Unit
        Path binPath =
            Awaitility.given()
                .ignoreException(HttpClientErrorException.NotFound.class)
                .until(
                    () ->
                        restClient.getBinaryObjectByUnitId(
                            tenant, tmpDir, acIdentifier, unitId, unit.getBinaryVersion()),
                    Objects::nonNull);

        assertNotNull(binPath);
        assertTrue(Files.exists(binPath));
        assertEquals(Files.size(unit.getBinaryPath()), Files.size(binPath));
        assertArrayEquals(
            HashUtils.checksum(Hash.SHA512, unit.getBinaryPath()),
            HashUtils.checksum(Hash.SHA512, binPath));
      }

      ResponseEntity<SearchResult<JsonNode>> r2 =
          restClient.getObjectMetadataByUnit(tenant, acIdentifier, Long.parseLong(unitId));

      assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
      SearchResult<JsonNode> result = r2.getBody();
      assertNotNull(result);

      JsonNode qualifier = result.results().getFirst().get("#qualifiers").get(0);
      assertEquals("BinaryMaster", qualifier.get("qualifier").asText());

      JsonNode version = qualifier.get("versions").get(0);
      assertEquals("BinaryMaster_1", version.get("DataObjectVersion").asText());
    }

    // Check binary objects from Json ATR
    Map<String, BinaryDataObjectReply> binaryMap = atr.getBinaryDataObjectReplyMap();
    for (BinaryDataObjectReply bdor : binaryMap.values()) {
      Path binPath =
          restClient.getBinaryObjectById(tenant, tmpDir, acIdentifier, bdor.getSystemId());

      assertNotNull(binPath);
      assertTrue(Files.exists(binPath));
      Hash hash = HashUtils.getHash(bdor.getDigestAlgorithm());
      assertEquals(bdor.getMessageDigest(), HashUtils.encodeHex(HashUtils.checksum(hash, binPath)));
    }
  }

  @Test
  void downloadXmlAtr(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    int identifier = 1;
    String acIdentifier = "AC-" + TestUtils.pad(identifier);

    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer smallSip = SipFactory.createSmallSip(tmpDir, 1);
    sedaService.write(smallSip, sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    // Download XML ATR
    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    // Retrieve all created archive units from ATR
    try (InputStream is = Files.newInputStream(atrPath)) {
      Element rootElem = new Builder().build(is).getRootElement();
      XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
      xc.addNamespace("ns", Sedav2Utils.SEDA_V21);

      // Check archive units from Xml ATR
      for (ArchiveUnit unit : smallSip.getArchiveUnits()) {
        // Get archive unit id (SystemId) from archive unit id attribute
        String query = "//ns:ArchiveUnit[@id='" + unit.getId() + "']";
        Nodes nodes = rootElem.query(query, xc);
        String unitId = nodes.get(0).getValue();
        if (unit.getBinaryPath() != null) {
          // Download binary object from Archive Unit
          Path binPath =
              Awaitility.given()
                  .ignoreException(HttpClientErrorException.NotFound.class)
                  .until(
                      () ->
                          restClient.getBinaryObjectByUnitId(
                              tenant, tmpDir, acIdentifier, unitId, unit.getBinaryVersion()),
                      Objects::nonNull);

          assertNotNull(binPath);
          assertTrue(Files.exists(binPath));
          assertEquals(Files.size(unit.getBinaryPath()), Files.size(binPath));
          assertArrayEquals(
              HashUtils.checksum(Hash.SHA512, unit.getBinaryPath()),
              HashUtils.checksum(Hash.SHA512, binPath));
        }
      }

      String q =
          """
            {
              "$roots":[ ],
              "$query":[
                {
                  "$match_all":{
                    "FileInfo.Filename":"hellolsimplesip_2.pdf"
                  }
                }
              ],
              "$filter":{ },
              "$projection":{ },
              "$facets":[ ]
            }
          """;
      ResponseEntity<SearchResult<JsonNode>> r2 =
          restClient.searchObjectMetadata(tenant, acIdentifier, q);
      assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
      SearchResult<JsonNode> result = r2.getBody();
      assertNotNull(result);

      JsonNode qualifier = result.results().getFirst().get("#qualifiers").get(0);
      JsonNode version = qualifier.get("versions").get(0);
      assertEquals("BinaryMaster", qualifier.get("qualifier").asText());
      assertEquals("BinaryMaster_1", version.get("DataObjectVersion").asText());
      assertEquals("hellolsimplesip_2.pdf", version.get("FileInfo").get("Filename").asText());
    }
  }

  @Test
  void ingestRngSip() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("FRAD01"));
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("DGFIP"));
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    ProfileDto profileDto = DtoFactory.createProfileDto("Matrice");
    ResponseEntity<List<ProfileDto>> r4 = restClient.createProfile(tenant, profileDto);
    assertEquals(HttpStatus.CREATED, r4.getStatusCode(), TestUtils.getBody(r4));

    ResponseEntity<Void> r5 =
        restClient.updateBinaryProfile(
            tenant,
            Paths.get(ItInit.PROFILE + "OK_profil_matrice.rng"),
            profileDto.getIdentifier());
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));

    IngestContractDto icDto = DtoFactory.createIngestContractDto("IC-000004");
    icDto.getArchiveProfiles().add("Matrice");
    ResponseEntity<List<IngestContractDto>> r3 = restClient.createIngestContract(tenant, icDto);
    assertEquals(HttpStatus.CREATED, r3.getStatusCode(), TestUtils.getBody(r3));

    ResponseEntity<Void> r6 =
        restClient.uploadSip(tenant, Paths.get(ItInit.SEDA_SIP + "sip_profile.zip"));
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));

    String requestId = r6.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());
  }

  @Test
  void ingestRng2Sip() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("FRAD01"));
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("DGFIP"));
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    ProfileDto profileDto = DtoFactory.createProfileDto("MatriceOld");
    ResponseEntity<List<ProfileDto>> r4 = restClient.createProfile(tenant, profileDto);
    assertEquals(HttpStatus.CREATED, r4.getStatusCode(), TestUtils.getBody(r4));

    ResponseEntity<Void> r5 =
        restClient.updateBinaryProfile(
            tenant,
            Paths.get(ItInit.PROFILE + "OK_profil_matriceOld.rng"),
            profileDto.getIdentifier());
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));

    IngestContractDto icDto = DtoFactory.createIngestContractDto("IC-000004");
    icDto.getArchiveProfiles().add("MatriceOld");
    ResponseEntity<List<IngestContractDto>> r3 = restClient.createIngestContract(tenant, icDto);
    assertEquals(HttpStatus.CREATED, r3.getStatusCode(), TestUtils.getBody(r3));

    ResponseEntity<Void> r6 =
        restClient.uploadSip(tenant, Paths.get(ItInit.SEDA_SIP + "sip_old.zip"));
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));

    String requestId = r6.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r6));
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));
  }

  @Test
  void ingestSimpleSip(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    String systemId = Scenario.createScenario04(restClient, tenant, userDto, tmpDir);
    String acIdentifier = "AC-" + TestUtils.pad(1);

    JsonNode archiveUnitDto =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(archiveUnitDto);
    assertEquals("MyTitle1", archiveUnitDto.get("Title").asText());
    assertEquals(1, archiveUnitDto.get("#version").asInt());
    assertEquals(0, archiveUnitDto.get("#lifecycles").size());
  }

  @Test
  void ingestComplexSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    int identifier = 1;
    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createComplexSip(tmpDir, identifier), sipPath);
    String acIdentifier = "AC-" + TestUtils.pad(identifier);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    String query =
        """
                {
                  "$roots": [],
                  "$query": [
                    {
                      "$and": [
                        {
                          "$wildcard": {
                            "Keyword.KeywordReference": "position*"
                          }
                        },
                        {
                          "$wildcard": {
                            "Keyword.KeywordContent": "SE*AL"
                          }
                        }
                      ],
                      "$depth": 2
                    }
                  ],
                  "$filter": {},
                  "$projection": {}
                }
                """;

    SearchResult<JsonNode> searchResult = searchArchive(tenant, acIdentifier, query);
    assertNotNull(searchResult, "Search Time out");

    assertEquals(1, searchResult.results().size());
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertEquals(1, searchResult.results().getFirst().get("#version").asInt());
    assertEquals(0, searchResult.results().getFirst().get("#lifecycles").size());
  }

  @Test
  void ingestLargeSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, 1, 1000), sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 30, RestClient.OP_FINAL);

    assertEquals(OperationStatus.OK, operation.status());
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
  }

  @Test
  void ingestFullTextSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer simpleSip = SipFactory.createSimpleSip(tmpDir, 1);
    ArchiveUnit archiveUnit = simpleSip.getArchiveUnits().getFirst();
    archiveUnit.addElement(
        new fr.xelians.sipg.model.Element(
            "FullText", StringUtils.repeat("Dummy_Text", " ", 10_000) + " THIS_IS_GOOD"));

    Path simplePath = tmpDir.resolve("sip.sip");
    sedaService.write(simpleSip, simplePath);
    ResponseEntity<?> response = restClient.uploadSip(tenant, simplePath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    String query =
        """
            {
              "$roots": [],
              "$query": [{
                  "$match_all": { "FullText": "THIS_IS_GOOD" }
                }
              ],
              "$filter": {},
              "$projection": {}
            }
            """;

    String acIdentifier = "AC-" + TestUtils.pad(1);
    SearchResult<JsonNode> searchResult = searchArchive(tenant, acIdentifier, query);
    assertNotNull(searchResult, "Search Time out");

    assertEquals(1, searchResult.results().size());
    assertEquals("MyTitle1", searchResult.results().getFirst().get("Title").asText());
    assertEquals(1, searchResult.results().getFirst().get("#version").asInt());
    assertEquals(0, searchResult.results().getFirst().get("#lifecycles").size());
  }

  // Attach SIP directly to parent SIP
  @Test
  void ingestUpdateOperationWithSystemId(@TempDir Path tmpDir)
      throws IOException, ParsingException {

    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer updateSip = SipFactory.createUpdateOperationSip(tmpDir, 1, systemId);
    sedaService.write(updateSip, sipPath);

    ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);
    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", Sedav2Utils.SEDA_V21);

    Nodes nodes = rootElem.query("//ns:ArchiveUnit[@id='UNIT_ID2']", xc);
    long unitId2 = Long.parseLong(nodes.get(0).getValue());
    nodes = rootElem.query("//ns:ArchiveUnit[@id='UNIT_ID3']", xc);
    long unitId3 = Long.parseLong(nodes.get(0).getValue());

    // Get unit and assert parent unit == systemId
    String query =
        """
                 {
                   "$roots": [],
                   "$query": [ { "$in": { "#id": [ "%s", "%s"] } } ],
                   "$filter": {},
                   "$projection": {}
                }
                """
            .formatted(unitId2, unitId3);

    String acIdentifier = "AC-" + TestUtils.pad(1);
    SearchResult<JsonNode> searchResult = searchArchives(tenant, acIdentifier, query, 2);
    assertNotNull(searchResult, "Search Time out");

    List<JsonNode> units = searchResult.results();
    assertEquals(2, units.size(), searchResult.toString());
    assertEquals(systemId, toLongs(units.getFirst().get("#unitups")).getFirst());
    assertEquals(systemId, toLongs(units.get(1).get("#unitups")).getFirst());
  }

  @Test
  void ingestUpdateOperationWithKeywords(@TempDir Path tmpDir)
      throws IOException, ParsingException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer simpleSip = SipFactory.createSimpleSipWithNowDate(tmpDir, 1);
    Map<String, Long> ids1 = Scenario.uploadSip(restClient, tenant, tmpDir, simpleSip);
    Long rootId = ids1.get("UNIT_ID1");
    Long linkId = ids1.get("UNIT_ID2");

    // Wait for indexing
    Utils.sleep(1000);

    ArchiveTransfer updateSip = SipFactory.createUpdateOperationSimpleSip(tmpDir, 1);
    Map<String, Long> ids2 = Scenario.uploadSip(restClient, tenant, tmpDir, updateSip);

    // Get unit and assert parent unit == systemId
    String idQuery =
        """
                     {
                       "$roots": [],
                       "$query": [ { "$in": { "#id": [ "%s", "%s"] } } ],
                       "$filter": {},
                       "$projection": {}
                    }
                    """
            .formatted(ids2.get("UNIT_ID2"), ids2.get("UNIT_ID3"));

    String acIdentifier = "AC-" + TestUtils.pad(1);
    SearchResult<JsonNode> searchResult = searchArchives(tenant, acIdentifier, idQuery, 2);
    assertNotNull(searchResult, "Search Time out");

    List<JsonNode> units = searchResult.results();
    assertEquals(2, units.size(), searchResult.toString());
    assertEquals(linkId, toLongs(units.getFirst().get("#unitups")).getFirst());
    assertEquals(linkId, toLongs(units.get(1).get("#unitups")).getFirst());

    assertEquals(linkId, toLongs(units.getFirst().get("#allunitups")).getFirst());
    assertEquals(linkId, toLongs(units.get(1).get("#allunitups")).getFirst());
    assertEquals(rootId, toLongs(units.getFirst().get("#allunitups")).get(1));
    assertEquals(rootId, toLongs(units.get(1).get("#allunitups")).get(1));
  }

  // Attach SIP to parent SIP via IngestContract
  @Test
  void ingestWithParentAttachment(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto(3));
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    // Wait for indexing
    Utils.sleep(1000);

    IngestContractDto ic2 = DtoFactory.createIngestContractDto(3);
    ic2.setLinkParentId(systemId);
    ResponseEntity<List<IngestContractDto>> r2 = restClient.createIngestContract(tenant, ic2);
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 3), sipPath);

    ResponseEntity<Void> r3 = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, r3.getStatusCode());

    String requestId = r3.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

    assertEquals(OperationStatus.OK, operation.status());
    // TODO Get Sip and assert parent == systemId
    assertEquals(HttpStatus.ACCEPTED, r3.getStatusCode());
  }

  @Test
  void ingestSeveralSips(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 1), sipPath);

    List<String> requestIds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      ResponseEntity<Void> response = restClient.uploadSip(tenant, sipPath);
      assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));
      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      requestIds.add(requestId);
    }

    String query =
        """
          {
              "id": "",
              "states": [ "COMPLETED" ],
              "statuses": [ "OK" ],
              "types": ["INGEST_ARCHIVE"],
              "startDateMin": "20/11/2020",
              "startDateMax": "21/11/2129"
          }
        """;

    long startTime = System.currentTimeMillis();

    while (true) {
      var response = restClient.searchOperations(tenant, query);
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      OperationResult<OperationDto> result = response.getBody();
      assertNotNull(result);

      if (result.results().stream()
          .map(OperationDto::id)
          .map(Object::toString)
          .collect(Collectors.toSet())
          .containsAll(requestIds)) break;

      assertFalse((System.currentTimeMillis() - startTime) > 60_000, "Operation Time Out");
      Utils.sleep(50);
    }
  }

  private SearchResult<JsonNode> searchArchives(Long tenant, String acId, String query, long min) {
    return Awaitility.await()
        .until(
            () -> restClient.searchArchive(tenant, acId, query),
            r -> r.getBody() != null && r.getBody().hits().size() >= min)
        .getBody();
  }

  private SearchResult<JsonNode> searchArchive(Long tenant, String acIdentifier, String query) {
    return searchArchives(tenant, acIdentifier, query, 1);
  }

  @Test
  void ingestSipWithAccessContract(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    String systemId = Scenario.createScenario04(restClient, tenant, userDto, tmpDir);
    String acIdentifier = "AC-" + TestUtils.pad(1);
    String agency1 = "AGENCY-" + TestUtils.pad(1);
    String agency2 = "AGENCY-" + TestUtils.pad(2);

    JsonNode archiveUnitDto =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(archiveUnitDto);
    assertEquals(agency1, archiveUnitDto.get("#originating_agency").asText());

    AccessContractDto accessContract200 = DtoFactory.createAccessContractDto(200);
    accessContract200.setOriginatingAgencies(new HashSet<>(Set.of(agency2)));
    accessContract200.setEveryOriginatingAgency(false);
    String acIdentifier200 = accessContract200.getIdentifier();
    var r1 = restClient.createAccessContract(tenant, accessContract200);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    userDto.getAccessContracts().add(new TenantContract(tenant, acIdentifier200));
    var r2 = restClient.updateUser(userDto);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    HttpClientErrorException t1 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getArchiveUnit(tenant, acIdentifier200, systemId));
    assertEquals(HttpStatus.NOT_FOUND, t1.getStatusCode(), t1.toString());
  }

  @Test
  void ingestChildSipWithAccessContract(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    String systemId = Scenario.createScenario04(restClient, tenant, userDto, tmpDir);
    String acIdentifier = "AC-" + TestUtils.pad(1);
    String agency1 = "AGENCY-" + TestUtils.pad(1);
    String agency2 = "AGENCY-" + TestUtils.pad(2);

    JsonNode archiveUnitDto =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(archiveUnitDto);
    assertEquals(agency1, archiveUnitDto.get("#originating_agency").asText());

    AccessContractDto accessContract200 = DtoFactory.createAccessContractDto(200);
    accessContract200.setOriginatingAgencies(new HashSet<>(Set.of(agency2)));
    accessContract200.setEveryOriginatingAgency(false);
    String acIdentifier200 = accessContract200.getIdentifier();
    var r1 = restClient.createAccessContract(tenant, accessContract200);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    userDto.getAccessContracts().add(new TenantContract(tenant, acIdentifier200));
    var r2 = restClient.updateUser(userDto);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    ArchiveTransfer updateSip = SipFactory.createUpdateOperationSimpleSip(tmpDir, 2);
    Map<String, Long> ids = Scenario.uploadSip(restClient, tenant, tmpDir, updateSip);

    for (var id : ids.values()) {
      Awaitility.given()
          .ignoreException(HttpClientErrorException.NotFound.class)
          .await()
          .until(
              () -> restClient.getArchiveUnit(tenant, acIdentifier200, id),
              r -> r.getStatusCode() == HttpStatus.OK);
    }

    String query =
        """
           {
             "$roots": [ ],
             "$query":{ "$exists":"#id" },
             "$filter": {},
             "$projection": {}
          }
          """;

    SearchResult<JsonNode> searchResult = searchArchives(tenant, acIdentifier, query, 5);
    assertNotNull(searchResult, "Search Time out");
    List<JsonNode> units = searchResult.results();
    assertEquals(5, units.size(), searchResult.toString());

    SearchResult<JsonNode> searchResult200 = searchArchives(tenant, acIdentifier200, query, 2);
    assertNotNull(searchResult200, "Search Time out");
    List<JsonNode> units200 = searchResult200.results();
    assertEquals(2, units200.size(), searchResult200.toString());
  }
}
