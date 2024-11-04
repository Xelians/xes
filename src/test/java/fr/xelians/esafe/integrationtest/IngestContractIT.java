/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.referential.dto.IngestContractDto;
import fr.xelians.esafe.referential.dto.ProfileDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class IngestContractIT extends BaseIT {

  private static final Path OK_PROFILE =
      Paths.get(ItInit.PROFILE + "OK_profil_ingestcontract.json");
  private static final Path OK_PROFILE2 = Paths.get(ItInit.PROFILE + "OK_profil_matrice.json");
  private static final Path OK_PROFILE3 = Paths.get(ItInit.PROFILE + "OK_profil_matriceOld.json");

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @BeforeEach
  void beforeEach() {
    // Nothing to init
  }

  @Test
  void createIngestContractTest() throws IOException {
    Path dir = Paths.get(ItInit.INGEST_CONTRACT);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".json")) {
      Long tenant = nextTenant();
      ResponseEntity<List<ProfileDto>> r1 = restClient.createProfile(tenant, OK_PROFILE);
      assertEquals(
          HttpStatus.CREATED,
          r1.getStatusCode(),
          String.format("path: %s - r1: %s", path, TestUtils.getBody(r1)));

      ResponseEntity<List<ProfileDto>> r2 = restClient.createProfile(tenant, OK_PROFILE2);
      assertEquals(
          HttpStatus.CREATED,
          r2.getStatusCode(),
          String.format("path: %s - r2: %s", path, TestUtils.getBody(r2)));

      ResponseEntity<List<ProfileDto>> r3 = restClient.createProfile(tenant, OK_PROFILE3);
      assertEquals(
          HttpStatus.CREATED,
          r3.getStatusCode(),
          String.format("path: %s - r3: %s", path, TestUtils.getBody(r3)));

      ResponseEntity<List<IngestContractDto>> r4 = restClient.createIngestContract(tenant, path);
      assertEquals(
          HttpStatus.CREATED,
          r4.getStatusCode(),
          String.format("path: %s - r4: %s", path, TestUtils.getBody(r4)));

      List<IngestContractDto> dtos = r4.getBody();
      assertNotNull(dtos, "path: " + path);

      for (IngestContractDto dto : dtos) {
        ResponseEntity<IngestContractDto> r5 =
            restClient.getIngestContractByIdentifier(tenant, dto.getIdentifier());
        assertEquals(
            HttpStatus.OK,
            r5.getStatusCode(),
            String.format("path: %s - r5: %s", path, TestUtils.getBody(r5)));
      }
    }
  }

  @Test
  void createBadIngestContractTest() throws IOException {
    Path dir = Paths.get(ItInit.INGEST_CONTRACT);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_", ".json")) {

      Long tenant = nextTenant();
      ResponseEntity<List<ProfileDto>> r1 = restClient.createProfile(tenant, OK_PROFILE);
      assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

      ResponseEntity<List<ProfileDto>> r2 = restClient.createProfile(tenant, OK_PROFILE2);
      assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

      ResponseEntity<List<ProfileDto>> r3 = restClient.createProfile(tenant, OK_PROFILE3);
      assertEquals(HttpStatus.CREATED, r3.getStatusCode(), TestUtils.getBody(r3));

      assertThrows(
          HttpClientErrorException.class,
          () -> restClient.createIngestContract(tenant, path),
          "Path: " + path.toString());
    }
  }

  @Test
  void getIngestContractTest() {
    Long tenant = nextTenant();

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<IngestContractDto>> response =
          restClient.createIngestContract(tenant, DtoFactory.createIngestContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    Map<String, Object> params = Map.of("sortby", "name", "sortdir", "desc");
    ResponseEntity<PageResult<IngestContractDto>> response2 =
        restClient.getIngestContracts(tenant, params);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    PageResult<IngestContractDto> dto = response2.getBody();

    assertEquals(0, dto.hits().offset());
    assertEquals(3, dto.results().size());
    assertEquals(3, dto.hits().size());
    assertEquals(20, dto.hits().limit());
    assertEquals(3, dto.hits().total());

    List<IngestContractDto> ics = dto.results();
    assertEquals("NAME-3", ics.get(0).getName());
    assertEquals("NAME-2", ics.get(1).getName());
    assertEquals("NAME-1", ics.get(2).getName());
  }

  @Test
  void searchIngestContractTest() {
    Long tenant = nextTenant();

    String query =
        """
             {
               "$query": [
                  {
                     "$neq": { "Identifier": "BAD_666" }
                 }
               ],
               "$filter": {
                   "$orderby": { "Identifier": -1 }
              },
               "$projection": {}
            }
            """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<IngestContractDto>> response =
          restClient.createIngestContract(tenant, DtoFactory.createIngestContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchIngestContracts(tenant, query);
    SearchResult<JsonNode> outputDtos2 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos2);
    assertEquals(3, outputDtos2.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos2.hits().total(), TestUtils.getBody(r3));
  }
}
