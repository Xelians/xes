/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.ROLE_ARCHIVE_MANAGER;
import static fr.xelians.esafe.organization.domain.Role.TenantRole.ROLE_ARCHIVE_READER;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.domain.TenantRole;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ArchiveExportIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void exportComplexSipTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    Scenario.createScenario02(restClient, tenant, userDto);

    // Init User
    userDto.getTenantRoles().add(new TenantRole(tenant, ROLE_ARCHIVE_MANAGER));
    userDto.getTenantRoles().add(new TenantRole(tenant, ROLE_ARCHIVE_READER));
    ResponseEntity<?> response = restClient.updateUser(userDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    restClient.signIn(restClient.getAccessKey());

    ArchiveTransfer sip = SipFactory.createComplexSip(tmpDir, 1);
    ArchiveTransfer[] sips = new ArchiveTransfer[10];
    Arrays.fill(sips, 0, sips.length, sip);
    Scenario.uploadSips(restClient, tenant, tmpDir, sips);

    // Update in order to create an LFC
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
                        { "$set": { "Description": "The new description of MyTitle1" }}
                    ]
                }
                """;

    ResponseEntity<String> r2 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    exportComplexSip1(tenant, tmpDir);
  }

  private void exportComplexSip1(Long tenant, Path tmpDir) throws IOException {

    String exportQuery =
        """
                {
                  "dipExportType": "FULL",
                  "dataObjectVersionToExport": {
                    "dataObjectVersions": [ "BinaryMaster", "Thumbnail"]
                  },
                  "transferWithLogBookLFC": true,
                  "dipRequestParameters": {
                    "messageRequestIdentifier": "MessageRequestIdentifier",
                    "requesterIdentifier": "RequesterIdentifier",
                    "archivalAgencyIdentifier": "ArchivalAgencyIdentifier",
                    "comment": "Test d'export",
                    "archivalAgreement": "ExportArchivalAgreement"
                  },
                  "dslRequest": {
                    "$roots": [],
                    "$type": "DOCTYPE-000001",
                    "$query": [
                      {
                        "$or": [
                          {
                            "$match": {
                              "Title": "MyTitle1"
                            }
                          },
                          {
                            "$match": {
                              "Title": "MyTitle2"
                            }
                          }
                        ]
                      }
                    ],
                    "$filter": {},
                    "$projection": {"$fields": { "Title": 1, "Description": 1, "Directeur.Nom": 1, "Directeur.Age": 1, "Code": 1 }}
                  },
                  "maxSizeThreshold": "1024",
                  "sedaVersion": "2.2"
                }
                """;

    ResponseEntity<String> r2 = restClient.exportArchive(tenant, acIdentifier, exportQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    Path dipPath = restClient.downloadDip(tenant, requestId, acIdentifier, tmpDir);
    // Files.copy(dipPath, Path.of("/tmp/export.zip"));

    assertNotNull(dipPath);
    assertTrue(Files.exists(dipPath));
    assertTrue(Files.size(dipPath) > 1000);
  }
}
