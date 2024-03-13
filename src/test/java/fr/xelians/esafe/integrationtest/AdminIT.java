/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.logbook.dto.LogbookOperationDto;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

@Execution(ExecutionMode.SAME_THREAD)
@Isolated
class AdminIT extends BaseIT {

  private UserDto userDto;

  @BeforeAll
  void beforeAll() throws IOException, ParsingException {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void rebuildIndex(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);
    String acIdentifier = "AC-" + TestUtils.pad(1);

    // Let enough time for indexing (TODO: optimize)
    Utils.sleep(1000);

    // Check Archive Unit exists
    ResponseEntity<JsonNode> response = restClient.getArchiveUnit(tenant, acIdentifier, systemId);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    JsonNode archiveUnitDto = response.getBody();

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createUpdateOperationSip(tmpDir, 1, systemId), sipPath);

    // Upload Sip
    ResponseEntity<Void> r2 = restClient.uploadSip(tenant, sipPath);
    final String requestId1 = r2.getHeaders().getFirst(X_REQUEST_ID);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));

    // Wait for async ingest operation from db
    OperationDto operation =
        restClient.waitForOperation(tenant, requestId1, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());
    // TODO Get Sip and assert parent == systemId

    // Let enough time for indexing (TODO: optimize)
    Utils.sleep(1000);

    // Get the logbook operation
    ResponseEntity<LogbookOperationDto> r3 = restClient.getLogbookOperation(tenant, requestId1);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    // Compare logbook operation and operation
    LogbookOperationDto logbookOperation = r3.getBody();

    assertNotNull(logbookOperation);
    assertEquals(operation.id(), logbookOperation.getId());
    assertEquals(operation.tenant(), logbookOperation.getTenant());
    assertEquals(operation.type(), logbookOperation.getType());

    // Wait a moment before closing the index while still writing
    // Utils.sleep(1000);

    // Create new empty index
    ResponseEntity<Object> r4 = restClient.newIndex(tenant);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));

    // Check operation does not exist in new index
    HttpClientErrorException t1 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getLogbookOperation(tenant, requestId1));
    assertEquals(HttpStatus.NOT_FOUND, t1.getStatusCode(), t1.toString());

    // Check archive does not exist in the new archive index
    HttpClientErrorException t2 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId));
    assertEquals(HttpStatus.NOT_FOUND, t2.getStatusCode(), t2.toString());

    // Update empty index
    ResponseEntity<Object> r7 = restClient.updateIndex(tenant);
    assertEquals(HttpStatus.ACCEPTED, r7.getStatusCode(), TestUtils.getBody(r7));

    // Wait for update index operation to finish
    final String requestId2 = r7.getHeaders().getFirst(X_REQUEST_ID);
    operation = restClient.waitForOperation(tenant, requestId2, 30, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    // Wait for Elastic to index
    Utils.sleep(1000);

    // Check operation exists in new logbook index
    ResponseEntity<LogbookOperationDto> r8 =
        restClient.getLogbookOperation(tenant, logbookOperation.getId());
    assertEquals(HttpStatus.OK, r8.getStatusCode(), TestUtils.getBody(r8));
    LogbookOperationDto newLogbookOperation = r8.getBody();

    assertNotNull(newLogbookOperation);
    assertEquals(logbookOperation.getId(), newLogbookOperation.getId());
    assertEquals(logbookOperation.getTenant(), newLogbookOperation.getTenant());
    assertEquals(logbookOperation.getType(), newLogbookOperation.getType());
    assertEquals(logbookOperation.getUserIdentifier(), newLogbookOperation.getUserIdentifier());
    assertEquals(logbookOperation.getTypeInfo(), newLogbookOperation.getTypeInfo());
    assertEquals(logbookOperation.getOutcome(), newLogbookOperation.getOutcome());
    assertEquals(logbookOperation.getObjectIdentifier(), newLogbookOperation.getObjectIdentifier());
    assertEquals(logbookOperation.getObjectData(), newLogbookOperation.getObjectData());
    assertEquals(logbookOperation.getObjectInfo(), newLogbookOperation.getObjectInfo());

    // Check if archive exists in the new archive index
    ResponseEntity<JsonNode> r9 = restClient.getArchiveUnit(tenant, acIdentifier, systemId);
    assertEquals(HttpStatus.OK, r9.getStatusCode(), TestUtils.getBody(r9));
    JsonNode newArchiveUnitDto = r9.getBody();

    assertNotNull(archiveUnitDto);
    assertNotNull(newArchiveUnitDto);
    assertEquals(archiveUnitDto.get("#opi").asLong(), newArchiveUnitDto.get("#opi").asLong());
  }
}
