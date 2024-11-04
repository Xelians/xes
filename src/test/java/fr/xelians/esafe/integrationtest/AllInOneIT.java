/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Header.X_REQUEST_ID;
import static org.junit.jupiter.api.Assertions.*;

import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.dto.OperationStatusDto;
import fr.xelians.esafe.referential.dto.*;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AllInOneIT extends BaseIT {

  public static final String ALL_IN_ONE = ItInit.RESOURCES + "all_in_one/";

  public static final String REF = ALL_IN_ONE + "ref/";
  public static final Path REF_DIR = Paths.get(REF);
  public static final Path RNG_DIR = REF_DIR.resolve("rng");

  public static final String HOL = ALL_IN_ONE + "holding/";
  public static final Path HOL_DIR = Paths.get(HOL);

  public static final String SIP = ALL_IN_ONE + "sip/";
  public static final Path SIP_DIR = Paths.get(SIP);

  public static final String AGENCY = "services_agents";
  public static final String RULE = "regles_gestion";
  public static final String PROFILE = "profil_archivage";
  public static final String ACCESS = "contrat_acces";
  public static final String INGEST = "contrat_entree";
  public static final String HOLDING = "arbre_de_positionnement";
  public static final String ARCHIVE = "sip_";

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void createAllInOneDataset() throws IOException {
    Long tenant = nextTenant();
    createCsvAgencies(tenant);
    createCsvRules(tenant);
    createProfiles(tenant);
    createAccessContracts(tenant);
    createIngestContracts(tenant);
    ingestHolding(tenant);
    Utils.sleep(2000);

    ingestSip(tenant);
  }

  void createCsvAgencies(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(REF_DIR, AGENCY, ".csv")) {
      ResponseEntity<List<AgencyDto>> response = restClient.createCsvAgency(tenant, path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  void createCsvRules(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(REF_DIR, RULE, ".csv")) {
      ResponseEntity<List<RuleDto>> response = restClient.createCsvRule(tenant, path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  private void createProfiles(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(REF_DIR, PROFILE, ".json")) {
      ResponseEntity<List<ProfileDto>> response = restClient.createProfile(tenant, path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
      assertNotNull(response.getBody());
      ProfileDto profile = response.getBody().getFirst();
      String profileIdentifier = profile.getIdentifier();

      String basename = FilenameUtils.getBaseName(path.toString());
      Path rngPath = RNG_DIR.resolve(basename + ".rng");
      ResponseEntity<Void> response2 =
          restClient.updateBinaryProfile(tenant, rngPath, profileIdentifier);
      assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    }
  }

  private void createAccessContracts(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(REF_DIR, ACCESS, ".json")) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), "path: " + path);
    }
  }

  private void createIngestContracts(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(REF_DIR, INGEST, ".json")) {
      ResponseEntity<List<IngestContractDto>> response =
          restClient.createIngestContract(tenant, path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), "path: " + path);
    }
  }

  private void ingestHolding(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContain(HOL_DIR, HOLDING, ".zip")) {
      ResponseEntity<Void> response = restClient.uploadHolding(tenant, path);
      assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, operation.status(), TestUtils.getBody(response));
    }
  }

  private void ingestSip(Long tenant) throws IOException {
    for (Path path : TestUtils.filenamesContainDeep(SIP_DIR, ARCHIVE, ".zip")) {
      System.err.println(path);
      ResponseEntity<Void> response = restClient.uploadSip(tenant, path);
      assertEquals(HttpStatus.ACCEPTED, response.getStatusCode(), TestUtils.getBody(response));

      String requestId = response.getHeaders().getFirst(X_REQUEST_ID);
      OperationStatusDto operation =
          restClient.waitForOperationStatus(tenant, requestId, 10, RestClient.OP_FINAL);
      assertEquals(OperationStatus.OK, operation.status());
    }
  }
}
