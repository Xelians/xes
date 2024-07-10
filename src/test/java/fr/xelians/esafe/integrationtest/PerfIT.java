/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fr.xelians.esafe.common.utils.Perfs;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.Scenario;
import fr.xelians.esafe.testcommon.SipFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class PerfIT extends BaseIT {

  private UserDto userDto;

  @BeforeAll
  void beforeAll() {
    SetupDto setupDto = setup();
    userDto = setupDto.userDto();
  }

  @BeforeEach
  void beforeEach() {}

  // @Test
  void ingestSimpleSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario01(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createSimpleSip(tmpDir, 1), sipPath);

    List<String> rs = Collections.synchronizedList(new ArrayList<>());

    Perfs perf = Perfs.start("Ingest_SimpleSip_3");

    IntStream.rangeClosed(1, 5000)
        .parallel()
        .forEach(
            i -> {
              try {
                ResponseEntity<Void> r = restClient.uploadSip(tenant, sipPath);
                assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
                String requestId = r.getHeaders().getFirst(X_REQUEST_ID);
                rs.add(requestId);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

    for (String r : rs) {
      OperationStatusDto ope =
          restClient.waitForOperationStatus(tenant, r, 120, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, ope.status(), r);
    }

    perf.log();
  }

  // @Test
  void ingestLargeSip(@TempDir Path tmpDir) throws IOException {
    Long tenant = nextTenant();
    Scenario.createScenario02(restClient, tenant, userDto);

    Path sipPath = tmpDir.resolve("sip.zip");
    sedaService.write(SipFactory.createLargeSip(tmpDir, 1, 5000), sipPath);

    List<String> rs = Collections.synchronizedList(new ArrayList<>());

    Perfs perf = Perfs.start("Ingest_LargeSip_5000");

    IntStream.rangeClosed(1, 200)
        .parallel()
        .forEach(
            i -> {
              try {
                ResponseEntity<Void> r = restClient.uploadSip(tenant, sipPath);
                assertEquals(HttpStatus.ACCEPTED, r.getStatusCode());
                String requestId = r.getHeaders().getFirst(X_REQUEST_ID);
                rs.add(requestId);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });

    for (String r : rs) {
      OperationStatusDto ope =
          restClient.waitForOperationStatus(tenant, r, 180, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, ope.status(), r);
    }

    perf.log();
  }
}
