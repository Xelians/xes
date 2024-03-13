/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.common.utils.JsonUtils.toLongs;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.atr.ArchiveTransferReply;
import fr.xelians.esafe.archive.domain.atr.ArchiveUnitReply;
import fr.xelians.esafe.archive.domain.atr.BinaryDataObjectReply;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationResult;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.esafe.referential.dto.ProfileDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.model.ArchiveUnit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nu.xom.*;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class IngestIT extends BaseIT {

  private UserDto userDto;

  @BeforeAll
  void beforeAll() throws IOException, ParsingException {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void createCsvAgenciesTest() throws IOException {
    Path dir = Paths.get(ItInit.AGENCY);
    for (Path path : TestUtils.listFiles(dir, "OK_", ".csv")) {
      Long tenant = nextTenant();
      ResponseEntity<List<AgencyDto>> response = restClient.createAgencies(tenant, path);
      assertEquals(
          HttpStatus.OK,
          response.getStatusCode(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(response)));
    }
  }

  @Test
  void ingestHoldingTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    ResponseEntity<List<AgencyDto>> r1 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("Identifier4"));
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("Identifier5"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    Path dir = Paths.get(ItInit.SEDA_HOLDING);
    for (Path path : TestUtils.listFiles(dir, "OK_arbre_", ".zip")) {
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

    // Wait for indexing
    Utils.sleep(1000);

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
    SearchResult<JsonNode> searchResult = searchArchive(tenant, acIdentifier, query);
    assertNotNull(searchResult, "Search Time out");

    List<JsonNode> units = searchResult.results();
    assertEquals(12, units.size(), searchResult.toString());
  }

  @Test
  void ingestHoldingKoTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path dir = Paths.get(ItInit.SEDA_HOLDING);
    for (Path path : TestUtils.listFiles(dir, "KO_arbre_", ".zip")) {
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

    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("Identifier4"));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("Identifier5"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    ResponseEntity<List<IngestContractDto>> r3 =
        restClient.createIngestContract(tenant, DtoFactory.createIngestContractDto("IC-000004"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    Path dir = Paths.get(ItInit.SEDA_FILING);
    for (Path path : TestUtils.listFiles(dir, "OK_plan_", ".zip")) {
      ResponseEntity<Void> r4 = restClient.uploadFiling(tenant, path);
      String requestId = r4.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

      assertEquals(
          OperationStatus.OK,
          operation.status(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(r4)));
      assertEquals(
          HttpStatus.ACCEPTED,
          r4.getStatusCode(),
          String.format("path: %s - response: %s", path, TestUtils.getBody(r4)));
    }
  }

  @Test
  void ingestFilingKoTest() throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path dir = Paths.get(ItInit.SEDA_FILING);
    for (Path path : TestUtils.listFiles(dir, "KO", ".zip")) {
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
  void downloadAtrFromBadSip(@TempDir Path tmpDir) throws IOException, ParsingException {
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
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    // Retrieve all created archive units from ATR
    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

    String replyCode = rootElem.query("//ns:ReplyCode", xc).get(0).getValue();
    assertEquals("KO", replyCode, Files.readString(atrPath, StandardCharsets.UTF_8));

    String eventType = rootElem.query("//ns:EventType", xc).get(0).getValue();
    assertEquals("INGEST_ARCHIVE", eventType, Files.readString(atrPath, StandardCharsets.UTF_8));

    String outcome = rootElem.query("//ns:Outcome", xc).get(0).getValue();
    assertEquals("ERROR_CHECK", outcome, Files.readString(atrPath, StandardCharsets.UTF_8));
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

    Utils.sleep(1000);

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
            restClient.getBinaryObjectByUnitId(
                tenant, tmpDir, acIdentifier, unitId, unit.getBinaryVersion());

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

    Utils.sleep(1000);

    // Download XML ATR
    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    // Retrieve all created archive units from ATR
    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

    // Check archive units from Xml ATR
    for (ArchiveUnit unit : smallSip.getArchiveUnits()) {
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
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("DGFIP"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    ResponseEntity<List<ProfileDto>> r4 =
        restClient.createProfile(tenant, DtoFactory.createProfileDto("Matrice"));
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));

    assertNotNull(r4.getBody());
    ProfileDto profile = r4.getBody().getFirst();
    ResponseEntity<Void> r5 =
        restClient.updateBinaryProfile(
            tenant, Paths.get(ItInit.PROFILE + "OK_profil_matrice.rng"), profile.getIdentifier());
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));

    IngestContractDto icDto = DtoFactory.createIngestContractDto("IC-000004");
    icDto.getArchiveProfiles().add("Matrice");
    ResponseEntity<List<IngestContractDto>> r3 = restClient.createIngestContract(tenant, icDto);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    ResponseEntity<Void> r6 =
        restClient.uploadSip(tenant, Paths.get(ItInit.SEDA_SIP + "sip_profile.zip"));
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));

    String requestId = r6.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

    assertEquals(OperationStatus.OK, operation.status());
    assertEquals(HttpStatus.ACCEPTED, r6.getStatusCode(), TestUtils.getBody(r6));
  }

  @Test
  void ingestRng2Sip() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<AgencyDto>> response =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("FRAD01"));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<List<AgencyDto>> r2 =
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto("DGFIP"));
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    ResponseEntity<List<ProfileDto>> r4 =
        restClient.createProfile(tenant, DtoFactory.createProfileDto("MatriceOld"));
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    assertNotNull(r4.getBody());
    ProfileDto profile = r4.getBody().getFirst();

    ResponseEntity<Void> r5 =
        restClient.updateBinaryProfile(
            tenant,
            Paths.get(ItInit.PROFILE + "OK_profil_matriceOld.rng"),
            profile.getIdentifier());
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));

    IngestContractDto icDto = DtoFactory.createIngestContractDto("IC-000004");
    icDto.getArchiveProfiles().add("MatriceOld");
    ResponseEntity<List<IngestContractDto>> r3 = restClient.createIngestContract(tenant, icDto);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

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

    // Let enough time for indexing (TODO: optimize)
    Utils.sleep(1000);

    ResponseEntity<JsonNode> r1 = restClient.getArchiveUnit(tenant, acIdentifier, systemId);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    JsonNode archiveUnitDto = r1.getBody();
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
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

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
    SearchResult<JsonNode> searchResult = searchArchive(tenant, acIdentifier, query);
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

    // Wait for indexing
    Utils.sleep(1000);

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
    SearchResult<JsonNode> searchResult = searchArchive(tenant, acIdentifier, idQuery);
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
        restClient.createAgencies(tenant, DtoFactory.createAgencyDto(2));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    // Wait for indexing
    Utils.sleep(1000);

    IngestContractDto ic2 = DtoFactory.createIngestContractDto(2);
    ic2.setLinkParentId(systemId);
    ResponseEntity<List<IngestContractDto>> r2 = restClient.createIngestContract(tenant, ic2);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 2), sipPath);

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

  private SearchResult<JsonNode> searchArchive(Long tenant, String acIdentifier, String query) {
    for (int i = 0; i < 50; i++) {
      Utils.sleep(50);
      var response = restClient.searchArchive(tenant, acIdentifier, query);
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
      SearchResult<JsonNode> searchResult = response.getBody();
      assertNotNull(searchResult);
      if (!searchResult.results().isEmpty()) return searchResult;
    }
    return null;
  }
}
