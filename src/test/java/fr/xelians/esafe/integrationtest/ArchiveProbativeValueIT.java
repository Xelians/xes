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

import fr.xelians.esafe.common.utils.Utils;
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

class ArchiveProbativeValueIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void probativeComplexSipTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    Long tenant = nextTenant();

    Scenario.createScenario02(restClient, tenant, userDto);

    // Init User
    userDto.getTenantRoles().add(new TenantRole(tenant, ROLE_ARCHIVE_MANAGER));
    userDto.getTenantRoles().add(new TenantRole(tenant, ROLE_ARCHIVE_READER));
    ResponseEntity<?> response = restClient.updateUser(userDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

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
                       "$match": { "Title": "MyTitle2" },
                       "$depth": 5
                     }
                   ],
                   "$filter": {},
                   "$action": [
                        { "$set": { "Description": "The new description of MyTitle2" }}
                    ]
                }
                """;

    ResponseEntity<String> r2 = restClient.updateArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    // Wait for securing
    Utils.sleep(2000);

    probativeComplexSip(tenant, tmpDir);
  }

  private void probativeComplexSip(Long tenant, Path tmpDir) {

    String query =
        """
                {
                  "dslQuery": {
                    "$roots": [],
                    "$type": "DOCTYPE-000001",
                    "$query": [
                      {
                        "$or": [
                          {
                            "$match": {
                              "Title": "MyTitle3"
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
                    "$projection": {}
                  },
                  "usage": [ "BinaryMaster" ],
                  "version": "LAST"
                }
                """;

    ResponseEntity<String> r2 = restClient.probativeValue(tenant, acIdentifier, query);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);

    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    // Get probative report
    Path repPath = tmpDir.resolve(requestId + ".probativevalue_report");
    restClient.downloadReport(tenant, requestId, repPath);
    assertTrue(Files.exists(repPath));

    // TODO add assert
    //    EliminationReport report = JsonService.to(repPath, EliminationReport.class);
    //    assertEquals(tenant, report.tenant());
    //    assertEquals(requestId, report.operationId().toString());
    //    assertEquals(ReportType.ELIMINATION, report.type());
    //    assertEquals(ReportStatus.OK, report.status());
    //    assertEquals(9, report.units().size());
  }
}
