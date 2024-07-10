/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TenantIT extends BaseIT {

  private UserDto userDto;

  @BeforeAll
  void beforeAll() throws IOException, ParsingException {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void createTenantTest() {
    TenantDto dto = DtoFactory.createTenantDto();
    ResponseEntity<List<TenantDto>> response = restClient.createTenants(dto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    List<TenantDto> outputDtos = response.getBody();
    assertNotNull(outputDtos);
    TenantDto outputDto = outputDtos.getFirst();

    assertEquals(1, outputDtos.size());
    assertEquals(dto.getDescription(), outputDto.getDescription());
    assertEquals(dto.getStatus(), outputDto.getStatus());
    assertEquals(dto.getCreationDate(), outputDto.getCreationDate());
    assertEquals(dto.getLastUpdate(), outputDto.getLastUpdate());
  }

  @Test
  void addStorageOffer(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();
    Scenario.createScenario04(restClient, tenant, userDto, tmpDir);

    ResponseEntity<TenantDto> r1 = restClient.getTenant(tenant);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Add Storage Offer to tenant
    ResponseEntity<String> r2 = restClient.addStorageOffer(tenant, "FS:FS02");
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));

    // Upload SIP
    ArchiveTransfer simpleSip = SipFactory.createSimpleSip(tmpDir, 1);
    Path sipPath = tmpDir.resolve("simplePath.sip");
    sedaService.write(simpleSip, sipPath);
    restClient.uploadSip(tenant, sipPath);

    // Wait for Add Storage Offer to finish
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status());

    // Check if tenant contains added Storage offer
    ResponseEntity<TenantDto> r3 = restClient.getTenant(tenant);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    TenantDto tenantDto = r3.getBody();

    assertNotNull(tenantDto);
    assertTrue(tenantDto.getStorageOffers().contains("FS:FS02"));

    // Check if system is coherent
    ResponseEntity<Object> r4 = restClient.checkCoherency(tenant, 1, 1);
    requestId = r4.getHeaders().getFirst(X_REQUEST_ID);
    operation = restClient.waitForOperationStatus(tenant, requestId, 240, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r4));
  }

  @Test
  void listTenantTest() {
    ResponseEntity<List<TenantDto>> response =
        restClient.createTenants(DtoFactory.createTenantDto());
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    response = restClient.listTenants();
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
  }
}
