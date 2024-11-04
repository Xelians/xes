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

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.domain.TenantRole;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.*;
import fr.xelians.sipg.model.ArchiveTransfer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UpdateRuleIT extends BaseIT {

  private final int identifier = 1;
  private final String acIdentifier = "AC-" + TestUtils.pad(identifier);
  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @Test
  void updateRuleTest(@TempDir Path tmpDir) throws IOException, ParsingException {
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
    Utils.sleep(1000);

    // Update in order to create an LFC
    String updateQuery =
        """
                 {
                 "dslRequest": {
                     "$roots": [],
                     "$query": [
                       {
                         "$match": { "Title": "MyTitle1" },
                         "$depth": 1
                       }
                     ],
                     "$filter": {}
                  },
                  "ruleActions": {
                      "add": [],
                      "update": [],
                      "delete": [
                        {
                          "AppraisalRule": {
                            "Rules": [
                              {
                                "Rule": "APPRAISALRULE-000001"
                              }
                            ]
                          }
                        },
                        {
                          "DisseminationRule": {
                            "Rules": [
                              {
                                "Rule": "DISSEMINATIONRULE-000002"
                              },
                              {
                                "Rule": "DISSEMINATIONRULE-000003"
                              }
                            ]
                          }
                        },
                        {
                          "StorageRule": {
                            "Rules": [
                              {
                                "Rule": "STORAGERULE-000001"
                              },
                              {
                                "Rule": "STORAGERULE-000002"
                              },
                              {
                                "Rule": "STORAGERULE-000003"
                              }
                            ]
                          }
                        }
                      ]
                    }
                 }

                """;

    ResponseEntity<String> r2 = restClient.updateRulesArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    String searchQuery =
        """
                    {
                      "$roots":[ ],
                      "$query":[
                        {
                          "$match":{
                            "Title":"%s"
                          }
                        }
                      ],
                      "$filter":{ },
                      "$projection":{ }
                    }
                    """;

    ResponseEntity<List<JsonNode>> r3 =
        restClient.searchArchiveStream(tenant, acIdentifier, searchQuery.formatted("MyTitle1"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    List<JsonNode> searchResult = r3.getBody();
    assertNotNull(searchResult);
    // assertEquals(10, searchResult.size(), TestUtils.getBody(r3));

    // TODO do real tests
  }

  @Test
  void updateRuleTest2(@TempDir Path tmpDir) throws IOException, ParsingException {
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
                     "dslRequest": {
                         "$roots": [],
                         "$query": [
                           {
                             "$match": { "Title": "MyTitle2" }
                           }
                         ],
                         "$filter": {}
                      },
                      "ruleActions": {
                          "add": [],
                          "update": [],
                          "delete": [
                            {
                              "DisseminationRule": {
                                "Rules": [
                                  {
                                    "Rule": "DISSEMINATIONRULE-000001"
                                  }
                                ]
                              }
                            },
                            {
                              "AccessRule": {
                                "Rules": [
                                  {
                                    "Rule": "ACCESSRULE-000001"
                                  },
                                  {
                                    "Rule": "ACCESSRULE-000002"
                                  },
                                  {
                                    "Rule": "ACCESSRULE-000003"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                     }

                    """;

    ResponseEntity<String> r2 = restClient.updateRulesArchive(tenant, acIdentifier, updateQuery);
    assertEquals(HttpStatus.ACCEPTED, r2.getStatusCode(), TestUtils.getBody(r2));
    String requestId = r2.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
    assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(r2));

    String searchQuery =
        """
                        {
                          "$roots":[ ],
                          "$query":[
                            {
                              "$match":{
                                "Title":"%s"
                              }
                            }
                          ],
                          "$filter":{ },
                          "$projection":{ }
                        }
                        """;

    ResponseEntity<List<JsonNode>> r3 =
        restClient.searchArchiveStream(tenant, acIdentifier, searchQuery.formatted("MyTitle2"));
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    List<JsonNode> searchResult = r3.getBody();
    assertNotNull(searchResult);
    // assertEquals(10, searchResult.size(), TestUtils.getBody(r3));
    // TODO add assert

  }
}
