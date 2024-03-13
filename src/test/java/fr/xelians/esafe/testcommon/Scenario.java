/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.testcommon;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.TenantContract;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.referential.dto.AccessContractDto;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.service.sedav2.Sedav2Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nu.xom.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Scenario {

  private Scenario() {}

  private static final Sedav2Service sedaService = Sedav2Service.getV22Instance();

  // Create referentials,
  public static void createScenario01(RestClient restClient, long tenant, UserDto userDto) {

    ResponseEntity<?> response =
        restClient.createOntologies(tenant, DtoFactory.createOntologyDto(1));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    response = restClient.createAgencies(tenant, DtoFactory.createAgencyDto(1));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    IngestContractDto ingestContractDto = DtoFactory.createIngestContractDto(1);
    response = restClient.createIngestContract(tenant, ingestContractDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    AccessContractDto accessContractDto = DtoFactory.createAccessContractDto(1);
    response = restClient.createAccessContract(tenant, accessContractDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    userDto.getAccessContracts().add(new TenantContract(tenant, accessContractDto.getIdentifier()));
    userDto.getIngestContracts().add(new TenantContract(tenant, ingestContractDto.getIdentifier()));

    response = restClient.updateUser(userDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
  }

  // Create referentials and rules
  public static void createScenario02(RestClient restClient, long tenant, UserDto userDto)
      throws IOException {

    Scenario.createScenario01(restClient, tenant, userDto);

    for (int i = 1; i <= 4; i++) {
      ResponseEntity<?> response = restClient.createRule(tenant, DtoFactory.createAccessRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createAppraisalRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createReuseRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createClassificationRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createDisseminationRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createStorageRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

      response = restClient.createRule(tenant, DtoFactory.createHoldRule(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  // Create referentials, upload holding, download the ATR the return the last SystemId from the ATR
  public static long createScenario03(
      RestClient restClient, long tenant, UserDto userDto, Path tmpDir)
      throws IOException, ParsingException {

    Scenario.createScenario01(restClient, tenant, userDto);

    ArchiveTransfer holding = SipFactory.createHolding(1);
    Path holdingPath = tmpDir.resolve("holding.sip");
    sedaService.write(holding, holdingPath);

    ResponseEntity<?> response = restClient.uploadHolding(tenant, holdingPath);
    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);

    response = restClient.getOperation(tenant, requestId);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(response));

    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");
    Nodes nodes =
        rootElem.query(
            "//ns:ArchiveUnit[@id='" + holding.getArchiveUnits().get(0).getId() + "']", xc);
    return Long.parseLong(nodes.get(0).getValue());
  }

  // Create referentials, rules, upload archive, download the ATR then return the archive SystemId
  // from the ATR
  public static String createScenario04(
      RestClient restClient, long tenant, UserDto userDto, Path tmpDir)
      throws IOException, ParsingException {

    Scenario.createScenario02(restClient, tenant, userDto);

    ArchiveTransfer simpleSip = SipFactory.createSimpleSip(tmpDir, 1);
    Path simplePath = tmpDir.resolve("sip.sip");
    sedaService.write(simpleSip, simplePath);
    ResponseEntity<?> response = restClient.uploadSip(tenant, simplePath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(response));

    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);

    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");
    Nodes nodes =
        rootElem.query(
            "//ns:ArchiveUnit[@id='" + simpleSip.getArchiveUnits().getFirst().getId() + "']", xc);
    return nodes.get(0).getValue();
  }

  // Get SystemId of the holding from the ATR, then link the ingest contract to this SystemId
  public static void createScenario05(
      RestClient restClient, long tenant, UserDto userDto, Path tmpDir)
      throws IOException, ParsingException {

    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    ResponseEntity<?> response = restClient.createAgencies(tenant, DtoFactory.createAgencyDto(2));
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    IngestContractDto ic2 = DtoFactory.createIngestContractDto(2);
    ic2.setLinkParentId(systemId);
    response = restClient.createIngestContract(tenant, ic2);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  public static Map<String, Long> uploadSip(
      RestClient restClient, long tenant, Path tmpDir, ArchiveTransfer sip)
      throws IOException, ParsingException {
    String requestId = upload(restClient, tenant, tmpDir, sip);
    return wait(restClient, tenant, tmpDir, requestId);
  }

  public static List<Map<String, Long>> uploadSips(
      RestClient restClient, long tenant, Path tmpDir, ArchiveTransfer... sips)
      throws IOException, ParsingException {

    List<String> requestIds = new ArrayList<>();
    List<Map<String, Long>> maps = new ArrayList<>();

    for (ArchiveTransfer sip : sips) {
      requestIds.add(upload(restClient, tenant, tmpDir, sip));
    }

    for (String requestId : requestIds) {
      maps.add(wait(restClient, tenant, tmpDir, requestId));
    }
    return maps;
  }

  private static String upload(RestClient restClient, long tenant, Path tmpDir, ArchiveTransfer sip)
      throws IOException {
    Path sipPath = Files.createTempFile(tmpDir, "test_", ".sip");
    sedaService.write(sip, sipPath);

    ResponseEntity<?> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));
    return response.getHeaders().getFirst(X_REQUEST_ID);
  }

  private static Map<String, Long> wait(
      RestClient restClient, long tenant, Path tmpDir, String requestId)
      throws IOException, ParsingException {
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    Path atrPath = tmpDir.resolve(requestId + ".atr");
    restClient.downloadXmlAtr(tenant, requestId, atrPath);
    Element rootElem = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(rootElem);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

    Map<String, Long> map = new HashMap<>();
    Nodes nodes = rootElem.query("//ns:ArchiveUnit", xc);
    for (Node node : nodes) {
      String id = ((Element) node).getAttributeValue("id");
      map.put(id, Long.parseLong(node.getValue()));
    }
    return map;
  }
}
