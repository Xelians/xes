/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.performancetest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.xelians.esafe.common.utils.Perfs;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.logbook.dto.VitamLogbookOperationDto;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationDto;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import fr.xelians.sipg.service.sedav2.Sedav2Service;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.xom.ParsingException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// Delete all buckets
// mc --help
// mc alias set myminio http://192.168.1.82:9000 hvA0vo3Nu8yruvQ3 VYo8QiN40bd4PKyC
// mc  rb --force --dangerous myminio

@Slf4j
class LogbookPEIT extends BasePEIT {

  private Sedav2Service sedaService;

  @BeforeAll
  void beforeAll() {
    sedaService = Sedav2Service.getInstance();
    super.signUpSignIn();
  }

  @BeforeEach
  void beforeEach() {}

  //    @Test
  void ingestHugeSip(@TempDir Path tmpDir) throws IOException {
    nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, 1, 5), sipPath);
    ingestSip(sipPath, 4);
  }

  @Test
  void ingestLargeSip(@TempDir Path tmpDir) throws IOException {

    nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    int num = 5000;
    int maxOp = 64;
    int loop = 2;

    Path sipPath = tmpDir.resolve("sip_" + num + ".zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, 1, num), sipPath);

    Perfs perfs = Perfs.start();
    for (int i = 0; i < loop; i++) {
      System.err.println("******************************************************************");
      System.err.println("********************       Ingest SIP             ****************");
      ingestSip(sipPath, maxOp);
    }
    perfs.log("Ingest all Sips");

    System.err.println("Wait for securing");
    Utils.sleep(1000);

    rebuildIndex(num * maxOp);
  }

  // @Test
  void ingestSimpleSipTest(@TempDir Path tmpDir) throws IOException, ParsingException {
    nextTenant();
    Scenario.createScenario05(restClient, tenant, userDto, tmpDir);

    Path sipPath = tmpDir.resolve("sip_simple.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 2), sipPath);

    ingestSip(sipPath, 10000);
  }

  void ingestSip(Path sipPath, int maxOp) throws IOException {
    log.info("SIP: {} - Number of operations: {}", sipPath.toString(), maxOp);
    Perfs perfs = Perfs.start();

    // Upload Sip
    List<String> requestIds = new ArrayList<>();

    ResponseEntity<?> response;
    for (int i = 0; i < maxOp; i++) {
      response = restClient.uploadSip(tenant, sipPath);
      assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
      requestIds.add(response.getHeaders().getFirst(X_REQUEST_ID));
    }
    perfs.log("Upload Sip");

    // Wait for ingest operation to complete
    List<OperationStatusDto> operations =
        restClient.waitForOperationsStatus(tenant, requestIds, 3600 * 100, OperationStatus.OK);
    operations.forEach(o -> assertEquals(OperationStatus.OK, o.status()));
    perfs.log("Ingest Sip");

    //    IngestionTask.checkPerf.logElapsed();
    //    IngestionTask.commitPerf.logElapsed();
    //    IngestionTask.indexPerf.logElapsed();

    // Wait for Elastic to index (TODO: loop)
    Utils.sleep(1000);

    // Compare search engine detail with db detail
    //    perfs.reset();
    //    operations.forEach(o -> {
    //      ResponseEntity<?> r = restClient.getLogbookOperation(tenant, o.getId());
    //      assertEquals(HttpStatus.OK, r.getStatusCode());
    //
    //      LogbookOperationDto logbookOperation = (LogbookOperationDto) r.getBody();
    //      assertEquals(o.getId(), logbookOperation.getId());
    //      assertEquals(o.getTenant(), logbookOperation.getTenant());
    //      assertEquals(o.getType(), logbookOperation.getType());
    //    });
    //    perfs.log("Assert Operation/Logbook");
  }

  private void rebuildIndex(int maxOp) {
    // Rebuild index
    Perfs perfs = Perfs.start("RebuildIndex");

    ResponseEntity<?> response = restClient.rebuildIndex(tenant);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    // Wait for async rebuild operation
    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationStatusDto operation =
        restClient.waitForOperationStatus(tenant, requestId, 3600 * 100, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());
    perfs.log("Rebuild index");

    // Wait for Elastic to index (TODO: loop)
    //        Utils.sleep(5000);

    // Check if operation exists in new index
    //        perfs.restart();
    //        operations.forEach(o -> {
    //            ResponseEntity<?> r = restClient.getLogbookOperation(tenant, o.getId());
    //            assertEquals(HttpStatus.OK, r.getStatusCode());
    //
    //            LogbookOperationDto logbookOperation = (LogbookOperationDto) r.getBody();
    //            assertEquals(o.getId(), logbookOperation.getId());
    //            assertEquals(o.getTenant(), logbookOperation.getTenant());
    //        });
    //        perfs.log("Assert Operation/Rebuild Logbook");
  }

  // @Test
  void rebuildLogbookIndex(@TempDir Path tmpDir) throws IOException, ParsingException {
    nextTenant();
    long systemId = Scenario.createScenario03(restClient, tenant, userDto, tmpDir);

    // Create Sip
    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createUpdateOperationSip(tmpDir, 1, systemId), sipPath);

    // Upload Sip
    ResponseEntity<?> response = restClient.uploadSip(tenant, sipPath);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    // Wait for async ingest operation from db
    String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    OperationDto operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);

    assertEquals(OperationStatus.OK, operation.status());
    // TODO Get Sip and assert parent == systemId

    // Wait for Elastic to index (TODO: loop)
    Utils.sleep(1000);

    // Get the logbook operation from Search engine
    response = restClient.getVitamLogbookOperation(tenant, requestId);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Compare search engine detail with db detail
    VitamLogbookOperationDto logbookOperation = (VitamLogbookOperationDto) response.getBody();
    assertEquals(operation.id(), logbookOperation.getId());
    assertEquals(operation.tenant(), logbookOperation.getTenant());
    assertEquals(operation.type().toString(), logbookOperation.getEvTypeProc());

    // Create new empty index
    response = restClient.newIndex(tenant);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Check if operation exists in new index
    response = restClient.getVitamLogbookOperation(tenant, requestId);
    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

    // Update empty index
    response = restClient.updateIndex(tenant);
    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

    // Wait for async ingest operation from db
    requestId = response.getHeaders().getFirst(X_REQUEST_ID);
    operation = restClient.waitForOperation(tenant, requestId, 10, OperationStatus.OK);
    assertEquals(OperationStatus.OK, operation.status());

    // Wait for Elastic to index (TODO: loop)
    Utils.sleep(10000);

    // Check if operation exists in new index
    response = restClient.getVitamLogbookOperation(tenant, logbookOperation.getId());
    assertEquals(HttpStatus.OK, response.getStatusCode());

    // Compare search engine detail with db detail
    VitamLogbookOperationDto logbookOperation2 = (VitamLogbookOperationDto) response.getBody();
    assertEquals(logbookOperation.getId(), logbookOperation2.getId());
    assertEquals(logbookOperation.getTenant(), logbookOperation2.getTenant());
    assertEquals(logbookOperation.getEvType(), logbookOperation2.getEvType());
  }
}
