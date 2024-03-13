/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.*;
import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.common.exception.LogbookExternalClientException;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.*;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.common.utils.DateUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.integrationtest.SetupDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.esafe.vitamtest.dto.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import fr.xelians.sipg.model.ArchiveUnit;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import nu.xom.*;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

class OperationIT extends VitamBaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);

  private UserDto userDto;

  @BeforeAll
  void beforeAll(@TempDir Path tmpDir) throws IOException {
    //    SecureClientConfigurationImpl config =
    //        new SecureClientConfigurationImpl("localhost", port, false, null, false);
    String filename = writeConfigurationFile(tmpDir.resolve("vitam-external.yaml"));
    IngestExternalClientFactory.changeMode(filename);
    AdminExternalClientFactory.changeModeFromFile(filename);
    AccessExternalClientFactory.changeMode(filename);

    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  private String writeConfigurationFile(Path path) throws IOException {
    Map<String, Object> data = new HashMap<>();
    data.put("serverHost", "localhost");
    data.put("serverPort", port);
    data.put("secure", false);
    data.put("hostnameVerification", false);

    Yaml yaml = new Yaml();
    yaml.dump(data, new FileWriter(path.toFile()));
    return path.toString();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void ingestSipTest(@TempDir Path tmpDir)
      throws IOException, IngestExternalException, VitamClientException, ParsingException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createSimpleSip(tmpDir, 1);
    sedaService.write(sip, sipPath);

    // Ingest Sip and check operation status
    String requestId = ingestSip(tenant, sipPath);
    ItemStatus itemStatus = getOperation(tenant, requestId);
    assertEquals(requestId, itemStatus.getItemId(), "requestId is not equal itemId");

    // Download Manifest
    Path mftPath = tmpDir.resolve(requestId + "_sip.mft");
    downloadAtr(tenant, requestId, mftPath);
    assertTrue(Files.exists(mftPath));
    assertTrue(Files.size(mftPath) > 500);

    // Download ATR
    Path atrPath = tmpDir.resolve(requestId + "_sip.atr");
    downloadAtr(tenant, requestId, atrPath);
    assertTrue(Files.exists(atrPath));
    assertTrue(Files.size(atrPath) > 500);

    Utils.sleep(1000);

    // Parse ATR
    Element root = new Builder().build(Files.newInputStream(atrPath)).getRootElement();
    XPathContext xc = XPathContext.makeNamespaceContext(root);
    xc.addNamespace("ns", "fr:gouv:culture:archivesdefrance:seda:v2.1");

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);
    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();

    List<String> unitIds = new ArrayList<>();
    sip.getArchiveUnits().forEach(unit -> visitUnits(unit, unitIds, root, xc));

    // Check archive units from ATR
    for (String unitId : unitIds) {
      AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
      try (AccessExternalClient client = factory.getClient()) {
        RequestResponse<JsonNode> r1 = client.selectUnitbyId(context, queryNode, unitId);
        assertTrue(r1.isOk());
        List<JsonNode> results = ((RequestResponseOK<JsonNode>) r1).getResults();
        assertEquals(1, results.size());
        assertTrue(results.getFirst().get("Title").asText().startsWith("MyTitle"));
      }
    }
  }

  private void visitUnits(ArchiveUnit unit, List<String> unitIds, Element root, XPathContext xc) {
    String query = "//ns:ArchiveUnit[@id='" + unit.getId() + "']";
    Nodes nodes = root.query(query, xc);
    unitIds.add(nodes.get(0).getValue());
    unit.getArchiveUnits().forEach(u -> visitUnits(u, unitIds, root, xc));
  }

  @Test
  void ingestSeveralSips(@TempDir Path tmpDir)
      throws IOException, IngestExternalException, VitamClientException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 1), sipPath);

    List<String> requestIds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      String requestId = ingestSip(tenant, sipPath);
      requestIds.add(requestId);
    }

    ProcessQuery query = new ProcessQuery();
    query.setStates(List.of(ProcessState.COMPLETED.toString()));
    query.setStatuses(List.of("OK"));
    query.setStartDateMin("20/11/2020");
    query.setStartDateMax("21/11/2129");

    long startTime = System.currentTimeMillis();

    while (true) {
      List<ProcessDetail> results = getOperations(tenant, query);
      if (results.stream()
          .map(ProcessDetail::getOperationId)
          .collect(Collectors.toSet())
          .containsAll(requestIds)) break;

      assertFalse((System.currentTimeMillis() - startTime) > 1000, "Operation Time Out");
      Utils.sleep(50);
    }
  }

  @Test
  void createExternalOperation() throws LogbookExternalClientException {
    Long tenant = nextTenant();

    LogbookOperationParameters params = LogbookParametersFactory.newLogbookOperationParameters();
    params
        .setStatus(StatusCode.KO)
        .setTypeProcess(LogbookTypeProcess.EXTERNAL_LOGBOOK)
        .putParameterValue(LogbookParameterName.eventIdentifier, "EBCDEF19765LIYT")
        .putParameterValue(LogbookParameterName.eventType, "EXT_CREATE_PROFILE")
        .putParameterValue(LogbookParameterName.eventDateTime, LocalDateTime.now().toString())
        .putParameterValue(LogbookParameterName.agentIdentifierApplicationSession, "ESAFE-TEST")
        .putParameterValue(LogbookParameterName.eventIdentifierProcess, "90d12f8f-416d-a724")
        .putParameterValue(LogbookParameterName.eventIdentifierRequest, "dkkdk8ajdjd-ARCGRT")
        .putParameterValue(LogbookParameterName.objectIdentifier, "6754AO")
        .putParameterValue(LogbookParameterName.objectIdentifierRequest, "profile")
        .putParameterValue(LogbookParameterName.eventDetailData, "{ value= '2'}")
        .putParameterValue(LogbookParameterName.outcome, "FAILED")
        .putParameterValue(LogbookParameterName.outcomeDetailMessage, "Create user failed");

    AdminExternalClientFactory factory = AdminExternalClientFactory.getInstance();
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());

    try (AdminExternalClient client = factory.getClient()) {
      RequestResponse response = client.createExternalOperation(context, params);
      int httpCode = response.getStatus();
      assertEquals(
          Response.Status.CREATED.getStatusCode(), httpCode, "Create external operation failed");
    }
  }

  @Test
  void searchLogbookTest(@TempDir Path tmpDir)
      throws IOException, IngestExternalException, VitamClientException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createSimpleSip(tmpDir, 1);
    sedaService.write(sip, sipPath);

    // Ingest Sip and check operation status
    String requestId = ingestSip(tenant, sipPath);
    ItemStatus itemStatus = getOperation(tenant, requestId);
    assertEquals(requestId, itemStatus.getItemId(), "requestId is not equal itemId");

    // Search for logbook operation
    Utils.sleep(1000);

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);

    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);

    ObjectNode queryNode = JsonNodeFactory.instance.objectNode();

    AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    try (AccessExternalClient client = factory.getClient()) {
      RequestResponse<LogbookOperation> r1 =
          client.selectOperationbyId(context, requestId, queryNode);

      assertTrue(r1.isOk());
      // assertEquals(r1.);
    }
  }

  @Test
  void searchUnitTest(@TempDir Path tmpDir)
      throws IOException,
          IngestExternalException,
          VitamClientException,
          InvalidCreateOperationException,
          InvalidParseOperationException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    sedaService.write(sip, sipPath);

    // Ingest Sip and check operation status
    for (int i = 0; i < 3; i++) {
      String requestId = ingestSip(tenant, sipPath);
      ItemStatus itemStatus = getOperation(tenant, requestId);
      assertEquals(requestId, itemStatus.getItemId(), "requestId is not equal itemId");
    }

    Utils.sleep(1000);

    // Search for data
    SelectMultiQuery query = new SelectMultiQuery();
    var q1 = match("Title", "MyTitle24");
    var q2 = and().add(match("Title", "MyTitle2"), not().add(match("Title", "MyTitle3")));
    var q3 = QueryHelper.or().add(q1, q2).setDepthLimit(2);
    query.setQuery(q3);
    query.setLimitFilter(0, 250);
    query.addUsedProjection("#id", "Title", "Description");

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);

    AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    try (AccessExternalClient client = factory.getClient()) {
      RequestResponse<JsonNode> r1 = client.selectUnits(context, query.getFinalSelect());

      assertTrue(r1.isOk());
      List<JsonNode> results = ((RequestResponseOK<JsonNode>) r1).getResults();
      assertEquals(3, results.size());
      assertEquals("MyTitle2", results.get(0).get("Title").asText());
      assertEquals("MyTitle2", results.get(1).get("Title").asText());
      assertEquals("MyTitle2", results.get(2).get("Title").asText());

      String unitId = results.get(0).get("#id").asText();
      String qualifier = BinaryQualifier.BinaryMaster.toString();
      Response r2 = client.getObjectStreamByUnitId(context, unitId, qualifier, 1);
      assertTrue(r1.isOk());

      try (InputStream is = r2.readEntity(InputStream.class)) {
        Path objectPath = Files.createTempFile(tmpDir, "tmp_", ".tmp");
        Files.copy(is, objectPath, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(objectPath));
        assertTrue((Files.size(objectPath) > 100));
      }
    }
  }

  @Test
  void searchUnitWithInheritedRuleTest(@TempDir Path tmpDir)
      throws IOException,
          IngestExternalException,
          VitamClientException,
          InvalidCreateOperationException,
          InvalidParseOperationException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    sedaService.write(sip, sipPath);

    // Ingest Sip and check operation status
    for (int i = 0; i < 3; i++) {
      String requestId = ingestSip(tenant, sipPath);
      ItemStatus itemStatus = getOperation(tenant, requestId);
      assertEquals(requestId, itemStatus.getItemId(), "requestId is not equal itemId");
    }

    Utils.sleep(1000);

    // Search for data
    SelectMultiQuery query = new SelectMultiQuery();
    var q1 = match("Title", "MyTitle24");
    var q2 = and().add(match("Title", "MyTitle2"), not().add(match("Title", "MyTitle3")));
    var q3 = QueryHelper.or().add(q1, q2).setDepthLimit(2);
    query.setQuery(q3);
    query.setLimitFilter(0, 250);
    query.addUsedProjection("#id", "Title", "Description");

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);

    AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    try (AccessExternalClient client = factory.getClient()) {
      RequestResponse<JsonNode> r1 =
          client.selectUnitsWithInheritedRules(context, query.getFinalSelect());

      assertTrue(r1.isOk());
      List<JsonNode> results = ((RequestResponseOK<JsonNode>) r1).getResults();
      assertEquals(3, results.size());
      assertEquals("MyTitle2", results.get(0).get("Title").asText());
      assertEquals("MyTitle2", results.get(1).get("Title").asText());
      assertEquals("MyTitle2", results.get(2).get("Title").asText());

      JsonNode firstNode = results.getFirst();
      assertNull(firstNode.get("#computedInheritedRules"));
      assertNotNull(firstNode.get("InheritedRules"));

      String unitId = firstNode.get("#id").asText();
      String qualifier = BinaryQualifier.BinaryMaster.toString();
      Response r2 = client.getObjectStreamByUnitId(context, unitId, qualifier, 1);
      assertTrue(r1.isOk());

      try (InputStream is = r2.readEntity(InputStream.class)) {
        Path objectPath = Files.createTempFile(tmpDir, "tmp_", ".tmp");
        Files.copy(is, objectPath, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(objectPath));
        assertTrue((Files.size(objectPath) > 100));
      }
    }
  }

  @Test
  void searchUnitAndObjectTest(@TempDir Path tmpDir)
      throws IOException, VitamClientException, InvalidCreateOperationException, ParsingException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sip, sip, sip);
    String[] id2s = ids.stream().map(m -> m.get("UNIT_ID2").toString()).toArray(String[]::new);

    Utils.sleep(1000);

    SelectMultiQuery select = new SelectMultiQuery();
    select.setQuery(in(VitamFieldsHelper.id(), id2s));
    select.addProjection(JsonHandler.createObjectNode());

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);

    AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    try (AccessExternalClient client = factory.getClient()) {
      RequestResponse<JsonNode> r1 =
          client.selectUnitsWithInheritedRules(context, select.getFinalSelect());

      assertTrue(r1.isOk());
      List<JsonNode> results = ((RequestResponseOK<JsonNode>) r1).getResults();
      assertEquals(3, results.size());
      assertEquals("MyTitle2", results.get(0).get("Title").asText());
      assertEquals("MyTitle2", results.get(1).get("Title").asText());
      assertEquals("MyTitle2", results.get(2).get("Title").asText());

      List<DescriptiveMetadataDto> dtos = to(r1.toJsonNode(), SearchResponseDto.class).getResults();
      DescriptiveMetadataDto dto = dtos.getFirst();
      assertEquals(dto.getId(), dto.getUnitObject());
      assertEquals("MyTitle2", dto.getTitle());
      assertEquals("Stored", dto.getStatus());
      assertEquals("INGEST", dto.getUnitType());

      QualifiersDto qualifiersDto = dto.getQualifiers().getFirst();
      assertEquals("BinaryMaster", qualifiersDto.getQualifier());
      assertEquals("1", qualifiersDto.getNbc());

      VersionsDto versionsDto = qualifiersDto.getVersions().getFirst();
      assertEquals("BinaryMaster_1", versionsDto.getDataObjectVersion());

      FileInfoModel fileInfoModel = versionsDto.getFileInfoModel();
      assertEquals("MyBinaryMasterFile.pdf", fileInfoModel.getFilename());
      assertEquals("Linux", fileInfoModel.getCreatingOs());

      FormatIdentificationModel format = versionsDto.getFormatIdentification();
      assertEquals("1.6", format.getFormatLitteral());
      assertEquals("fmt/20", format.getFormatId());
      assertEquals("application/pdf", format.getMimeType());

      InheritedRulesDto inheritedDto = dto.getInheritedRules();
      InheritedRuleCategoryDto accessRuleDto = inheritedDto.getAccessRule();
      assertTrue(accessRuleDto.getRules().isEmpty());
      assertTrue(accessRuleDto.getProperties().isEmpty());

      InheritedRuleCategoryDto appraisalRuleDto = inheritedDto.getAppraisalRule();
      assertEquals("AGENCY-000001", appraisalRuleDto.getRules().getFirst().getOriginatingAgency());
      assertEquals("APPRAISALRULE-000001", appraisalRuleDto.getRules().getFirst().getRule());
      assertEquals("FinalAction", appraisalRuleDto.getProperties().getFirst().getPropertyName());
      assertEquals("Destroy", appraisalRuleDto.getProperties().getFirst().getPropertyValue());

      ComputedInheritedRulesDto computedInheritedRules = dto.getComputedInheritedRules();
      ComputedInheritedRuleDto computedAccessRuleDto = computedInheritedRules.getAccessRule();
      assertTrue(DateUtils.isLocalDate(computedAccessRuleDto.getMaxEndDate()));

      AppraisalComputedInheritedRuleDto computedAppraisalRuleDto =
          computedInheritedRules.getAppraisalRule();
      assertTrue(DateUtils.isLocalDate(computedAppraisalRuleDto.getMaxEndDate()));
      assertEquals("Destroy", computedAppraisalRuleDto.getFinalAction().getFirst());
    }
  }

  @Test
  void searchObjectTest(@TempDir Path tmpDir)
      throws IOException, VitamClientException, InvalidCreateOperationException, ParsingException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    List<Map<String, Long>> ids = Scenario.uploadSips(restClient, tenant, tmpDir, sip, sip, sip);
    String[] id2s = ids.stream().map(m -> m.get("UNIT_ID2").toString()).toArray(String[]::new);

    Utils.sleep(1000);

    SelectMultiQuery select = new SelectMultiQuery();
    select.setQuery(in(VitamFieldsHelper.id(), id2s));
    select.addProjection(JsonHandler.createObjectNode());

    final int identifier = 1;
    final String acIdentifier = "AC-" + TestUtils.pad(identifier);
    VitamContext context = new VitamContext(tenant.intValue());
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());
    context.setAccessContract(acIdentifier);

    AccessExternalClientFactory factory = AccessExternalClientFactory.getInstance();
    try (AccessExternalClient client = factory.getClient()) {
      RequestResponse<JsonNode> r1 = client.selectObjects(context, select.getFinalSelect());

      assertTrue(r1.isOk());
      List<JsonNode> results = ((RequestResponseOK<JsonNode>) r1).getResults();
      assertEquals(3, results.size());

      List<DescriptiveMetadataDto> dtos = to(r1.toJsonNode(), SearchResponseDto.class).getResults();
      DescriptiveMetadataDto dto = dtos.getFirst();
      QualifiersDto qualifiersDto = dto.getQualifiers().getFirst();
      assertEquals("BinaryMaster", qualifiersDto.getQualifier());
      assertEquals("1", qualifiersDto.getNbc());

      VersionsDto versionsDto = qualifiersDto.getVersions().getFirst();
      assertEquals("BinaryMaster_1", versionsDto.getDataObjectVersion());

      FileInfoModel fileInfoModel = versionsDto.getFileInfoModel();
      assertEquals("MyBinaryMasterFile.pdf", fileInfoModel.getFilename());
      assertEquals("Linux", fileInfoModel.getCreatingOs());

      FormatIdentificationModel format = versionsDto.getFormatIdentification();
      assertEquals("1.6", format.getFormatLitteral());
      assertEquals("fmt/20", format.getFormatId());
      assertEquals("application/pdf", format.getMimeType());
    }
  }

  @Test
  void downloadObjectTest(@TempDir Path tmpDir)
      throws IOException, IngestExternalException, VitamClientException {

    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    sedaService.write(sip, sipPath);

    // Ingest Sip and check operation status
    for (int i = 0; i < 3; i++) {
      String requestId = ingestSip(tenant, sipPath);
      ItemStatus itemStatus = getOperation(tenant, requestId);
      assertEquals(requestId, itemStatus.getItemId(), "requestId is not equal itemId");
    }

    Utils.sleep(1000);
  }

  private ItemStatus getOperation(long tenant, String requestId) throws VitamClientException {
    AdminExternalClientFactory factory = AdminExternalClientFactory.getInstance();
    VitamContext context = new VitamContext((int) tenant);
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());

    long startTime = System.currentTimeMillis();

    try (AdminExternalClient client = factory.getClient()) {
      while (true) {
        RequestResponse<ItemStatus> response =
            client.getOperationProcessExecutionDetails(context, requestId);

        if (response.isOk()) {
          ItemStatus item = ((RequestResponseOK<ItemStatus>) response).getFirstResult();
          if (item.getGlobalState() == ProcessState.COMPLETED) {
            return item;
          }
          assertFalse((System.currentTimeMillis() - startTime) > 1000, "Operation Time Out");
          assertFalse(item.getGlobalStatus().isGreaterOrEqualToKo(), "Operation fatal error");
        }
        Utils.sleep(50);
      }
    }
  }

  private List<ProcessDetail> getOperations(long tenant, ProcessQuery query)
      throws VitamClientException {
    AdminExternalClientFactory factory = AdminExternalClientFactory.getInstance();
    VitamContext context = new VitamContext((int) tenant);
    context.setApplicationSessionId("ESAFE|!|" + "Bearer " + restClient.getAccessToken());

    try (AdminExternalClient client = factory.getClient()) {
      RequestResponse<ProcessDetail> response = client.listOperationsDetails(context, query);
      assertTrue(response.isOk(), "Get operations is not ok");
      return ((RequestResponseOK<ProcessDetail>) response).getResults();
    }
  }

  private String ingestSip(long tenant, Path path) throws IOException, IngestExternalException {
    IngestExternalClientFactory ingestFactory = IngestExternalClientFactory.getInstance();
    VitamContext context = new VitamContext((int) tenant);
    context.setApplicationSessionId("ESAFETEST|!|" + "Bearer " + restClient.getAccessToken());

    try (IngestExternalClient client = ingestFactory.getClient();
        InputStream is = Files.newInputStream(path)) {
      RequestResponse<Void> response = client.ingest(context, is, "DEFAULT_WORKFLOW", "RESUME");
      assertTrue(response.isOk(), "response is not ok for " + path);
      String requestId = response.getVitamHeaders().get(X_REQUEST_ID);
      assertNotNull(requestId, "requestId is null for " + path);
      return requestId;
    }
  }

  private void downloadAtr(long tenant, String requestId, Path path)
      throws VitamClientException, IOException {
    IngestExternalClientFactory ingestFactory = IngestExternalClientFactory.getInstance();
    VitamContext context = new VitamContext((int) tenant);
    context.setApplicationSessionId("ESAFETEST|!|" + "Bearer " + restClient.getAccessToken());

    var ATR = IngestCollection.ARCHIVETRANSFERREPLY;
    try (IngestExternalClient client = ingestFactory.getClient();
        Response response = client.downloadObjectAsync(context, requestId, ATR)) {
      InputStream is = response.readEntity(InputStream.class);
      Files.copy(is, path);
    }
  }

  private void downloadManifest(long tenant, String requestId, Path path)
      throws VitamClientException, IOException {
    IngestExternalClientFactory ingestFactory = IngestExternalClientFactory.getInstance();
    VitamContext context = new VitamContext((int) tenant);
    context.setApplicationSessionId("ESAFETEST|!|" + "Bearer " + restClient.getAccessToken());

    var MFT = IngestCollection.MANIFESTS;
    try (IngestExternalClient client = ingestFactory.getClient();
        Response response = client.downloadObjectAsync(context, requestId, MFT)) {
      InputStream is = response.readEntity(InputStream.class);
      Files.copy(is, path);
    }
  }

  public static <T> T to(JsonNode node, Class<T> klass) throws JsonProcessingException {
    Validate.notNull(node, Utils.NOT_NULL, "node");

    ObjectMapper readMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    readMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    readMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    readMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    ObjectReader objectReader = readMapper.reader();
    return objectReader.treeToValue(node, klass);
  }
}
