/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
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
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
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
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void rebuildIndex(@TempDir Path tmpDir) throws IOException, ParsingException {
    userDto = signInAsRootAdmin();
    Long tenant = nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);
    String acIdentifier = "AC-" + TestUtils.pad(1);

    // Check Archive Unit exists
    var response =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId),
                r -> r.getStatusCode() == HttpStatus.OK);
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

    // Get the logbook operation
    VitamLogbookOperationDto logbookOperation =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getVitamLogbookOperation(tenant, requestId1),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    // Compare logbook operation and operation
    assertNotNull(logbookOperation);
    assertEquals(operation.id(), logbookOperation.getId());
    assertEquals(operation.tenant(), logbookOperation.getTenant());
    assertEquals(operation.applicationId(), logbookOperation.getEvIdAppSession());
    assertEquals(operation.userIdentifier(), logbookOperation.getAgId());

    // Create new empty index
    ResponseEntity<Object> r4 = restClient.newIndex(tenant);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));

    // Check operation does not exist in new index
    HttpClientErrorException t1 =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getVitamLogbookOperation(tenant, requestId1));
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

    // Check operation exists in new logbook index
    VitamLogbookOperationDto newLogbookOperation =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getVitamLogbookOperation(tenant, logbookOperation.getId()),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(newLogbookOperation);
    assertEquals(logbookOperation.getId(), newLogbookOperation.getId());
    assertEquals(logbookOperation.getTenant(), newLogbookOperation.getTenant());
    assertEquals(logbookOperation.getEvType(), newLogbookOperation.getEvType());
    assertEquals(logbookOperation.getEvIdAppSession(), newLogbookOperation.getEvIdAppSession());
    assertEquals(logbookOperation.getAgIdApp(), newLogbookOperation.getAgIdApp());
    assertEquals(logbookOperation.getEvTypeProc(), newLogbookOperation.getEvTypeProc());
    assertEquals(logbookOperation.getOutcome(), newLogbookOperation.getOutcome());
    assertEquals(logbookOperation.getObId(), newLogbookOperation.getObId());
    assertEquals(logbookOperation.getEvDetData(), newLogbookOperation.getEvDetData());
    assertEquals(logbookOperation.getObIdReq(), newLogbookOperation.getObIdReq());

    // Check if archive exists in the new archive index
    JsonNode newArchiveUnitDto =
        Awaitility.given()
            .ignoreException(HttpClientErrorException.NotFound.class)
            .await()
            .until(
                () -> restClient.getArchiveUnit(tenant, acIdentifier, systemId),
                r -> r.getStatusCode() == HttpStatus.OK)
            .getBody();

    assertNotNull(archiveUnitDto);
    assertNotNull(newArchiveUnitDto);
    assertEquals(archiveUnitDto.get("#opi").asLong(), newArchiveUnitDto.get("#opi").asLong());
  }
}
