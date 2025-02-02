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
import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.referential.dto.OntologyDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class OntologyIT extends BaseIT {

  private static final Path ONTOLOGY1 = Paths.get(ItInit.ONTOLOGY + "OK_indexmap_1.json");
  private static final Path ONTOLOGY2 = Paths.get(ItInit.ONTOLOGY + "OK_indexmap_2.json");
  private static final Path ONTOLOGY_NO_ID1 = Paths.get(ItInit.ONTOLOGY + "OK_indexmap_4.json");
  private static final Path ONTOLOGY_NO_ID2 = Paths.get(ItInit.ONTOLOGY + "OK_indexmap_5.json");
  private static final Path BAD_ONTOLOGY = Paths.get(ItInit.ONTOLOGY + "KO_indexmap_1.json");

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void createOneOntologyTest() throws IOException {
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);

    Long tenant = nextTenant();
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    List<OntologyDto> outputDtos = response.getBody();
    assertNotNull(outputDtos);
    assertEquals(ontologyDtos.size(), outputDtos.size());

    HashMap<String, OntologyDto> map = new HashMap<>();
    outputDtos.forEach(dto -> map.put(dto.getIdentifier(), dto));
    for (var dto : ontologyDtos) {
      var outputDto = map.get(dto.getIdentifier());
      assertNotNull(outputDto);
      assertEquals(dto.getName(), outputDto.getName());
      assertEquals(dto.getStatus(), outputDto.getStatus());
      assertNotEquals(dto.getLastUpdate(), outputDto.getLastUpdate());
      // TODO dedup && sort && compare
      // assertEquals(Utils.sortedList(dto.getMappings(), c),
      // Utils.sortedList(outputDto.getMappings(), c));
      map.remove(dto.getIdentifier());
    }
  }

  @Test
  void createOntologyTest() throws IOException {
    Path dir = Paths.get(ItInit.ONTOLOGY);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".json")) {
      ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(nextTenant(), path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  @Test
  void createOneBadOntologyTest() {
    Long tenant = nextTenant();
    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.createOntologies(tenant, BAD_ONTOLOGY));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void createBadOntologyMapTest() throws IOException {
    Path dir = Paths.get(ItInit.ONTOLOGY);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_", ".json")) {
      Long tenant = nextTenant();
      HttpClientErrorException thrown =
          assertThrows(
              HttpClientErrorException.class, () -> restClient.createOntologies(tenant, path));
      assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
    }
  }

  @Test
  void createTwoOntologyTest() throws IOException {
    Long tenant = nextTenant();

    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class, () -> restClient.createOntologies(tenant, ONTOLOGY1));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void createNoIdOntologyTest() throws IOException {
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response =
        restClient.createOntologies(nextTenant(), ONTOLOGY_NO_ID1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    List<OntologyDto> outputDtos = response.getBody();
    assertNotNull(outputDtos);
    assertEquals(ontologyDtos.size(), outputDtos.size());
  }

  @Test
  void createTwoNoIdOntologyTest() throws IOException {
    Long tenant = nextTenant();

    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response =
        restClient.createOntologies(tenant, ONTOLOGY_NO_ID1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    List<OntologyDto> outputDtos1 = response.getBody();

    response = restClient.createOntologies(tenant, ONTOLOGY_NO_ID2);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    List<OntologyDto> outputDtos2 = response.getBody();

    assertNotNull(outputDtos1);
    assertNotNull(outputDtos2);
    assertEquals(ontologyDtos.size() * 2, outputDtos1.size() + outputDtos2.size());
  }

  @Test
  void getAllOntologiesTest() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos1 = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    List<OntologyDto> ontologyDtos2 = DtoFactory.createOntologyDtos(ONTOLOGY2);
    response = restClient.createOntologies(tenant, ONTOLOGY2);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologies(tenant);
    PageResult<OntologyDto> outputDtos3 = response2.getBody();
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertEquals(ontologyDtos1.size() + ontologyDtos2.size(), outputDtos3.hits().total());
  }

  @Test
  void searchOntologiesTest() throws IOException {
    Long tenant = nextTenant();

    String query =
        """
             {
               "$query": [
                  {
                    "$and": [
                       {
                         "$in": { "ActivationDate": ["1969-02-27", "1970-01-01"] }
                       },
                       {
                         "$eq": { "Status": "ACTIVE" }
                       }
                    ]
                  }
               ],
               "$filter": {
                   "$offset": 0,
                   "$limit": 1,
                   "$orderby": { "Identifier": -1 }
              },
               "$projection": {"$fields": { "#tenant": 1 , "Identifier": 1,  "Name": 1 , "CreationDate": 1, "Status": 1 }}
            }
            """;

    ResponseEntity<List<OntologyDto>> r1 = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<List<OntologyDto>> r2 = restClient.createOntologies(tenant, ONTOLOGY2);
    assertEquals(HttpStatus.CREATED, r2.getStatusCode(), TestUtils.getBody(r2));

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchOntologies(tenant, query);
    SearchResult<JsonNode> result3 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    assertNotNull(result3);

    JsonNode result = result3.results().getFirst();
    assertEquals(1, result3.results().size(), TestUtils.getBody(r3));
    assertEquals(2, result3.hits().total(), TestUtils.getBody(r3));
    assertEquals(tenant, result.get("#tenant").asLong(), TestUtils.getBody(r2));
  }

  @Test
  void getOntologiesByStatusTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    Map<String, Object> params =
        Map.of("status", Status.INACTIVE, "sortby", "name", "sortdir", "desc");
    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologies(tenant, params);
    PageResult<OntologyDto> outputDtos3 = response2.getBody();
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertNotNull(outputDtos3);
    assertEquals(0, outputDtos3.hits().total());
  }

  @Test
  void getOntologiesByStatusTest2() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    Map<String, Object> params =
        Map.of("status", Status.ACTIVE, "sortby", "name", "sortdir", "desc");
    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologies(tenant, params);
    PageResult<OntologyDto> outputDtos3 = response2.getBody();
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertNotNull(outputDtos3);
    assertEquals(ontologyDtos.size(), outputDtos3.hits().total());
  }

  @Test
  void getOntologiesByNameTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    Map<String, Object> params =
        Map.of("name", "ZzzzzzZ", "sortby", "creationDate", "sortdir", "desc");
    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologies(tenant, params);
    PageResult<OntologyDto> outputDtos3 = response2.getBody();
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertNotNull(outputDtos3);
    assertEquals(0, outputDtos3.hits().total());
  }

  @Test
  void getOntologiesByNameTest2() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    var ontologyDto = ontologyDtos.getFirst();
    String name = ontologyDto.getName();
    Map<String, Object> params = Map.of("name", name, "sortby", "creationDate", "sortdir", "asc");
    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologies(tenant, params);
    PageResult<OntologyDto> outputDtos3 = response2.getBody();
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertNotNull(outputDtos3);
    assertEquals(ontologyDtos.size(), outputDtos3.hits().total());
  }

  @Test
  void getOntologiesByNameTest3() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    var ontologyDto = ontologyDtos.getFirst();
    String name = ontologyDto.getName();
    ResponseEntity<PageResult<OntologyDto>> response2 = restClient.getOntologyByName(tenant, name);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    OntologyDto outputDtos2 = response2.getBody().results().getFirst();
    assertEquals(name, outputDtos2.getName());
  }

  @Test
  void getOntologiesByIdentifierTest() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    var ontologyDto = ontologyDtos.getFirst();
    String identifier = ontologyDto.getIdentifier();
    ResponseEntity<OntologyDto> response2 = restClient.getOntologyByIdentifier(tenant, identifier);
    OntologyDto outputDtos2 = response2.getBody();

    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    assertNotNull(outputDtos2);
    assertEquals(ontologyDto.getName(), outputDtos2.getName());
    assertEquals(ontologyDto.getStatus(), outputDtos2.getStatus());
    assertNotEquals(ontologyDto.getCreationDate(), outputDtos2.getCreationDate());
    assertNotEquals(ontologyDto.getLastUpdate(), outputDtos2.getLastUpdate());
  }

  @Test
  void getOneOntologyBadTenantTest() throws IOException {

    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response =
        restClient.createOntologies(nextTenant(), ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    Long tenant = nextTenant();
    String identifier = ontologyDtos.getFirst().getIdentifier();
    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getOntologyByIdentifier(tenant, identifier));
    assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void getOneOntologyBadIdentifierTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.getOntologyByIdentifier(tenant, "BadIdentifier"));
    assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void updateOntologyIdentifierTest() throws IOException {
    Long tenant = nextTenant();

    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    OntologyDto indexMap = ontologyDtos.getFirst();
    String identifier = indexMap.getIdentifier();
    indexMap.setIdentifier(identifier + "_BAD");
    HttpClientErrorException thrown =
        assertThrows(
            HttpClientErrorException.class,
            () -> restClient.updateOntology(tenant, indexMap, identifier));
    assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
  }

  @Test
  void updateOntologyNameTest() throws IOException {
    Long tenant = nextTenant();
    List<OntologyDto> ontologyDtos = DtoFactory.createOntologyDtos(ONTOLOGY1);
    ResponseEntity<List<OntologyDto>> response = restClient.createOntologies(tenant, ONTOLOGY1);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));

    OntologyDto ontologyDto = ontologyDtos.getFirst();
    ontologyDto.setName("NewIndexMapName");

    ResponseEntity<OntologyDto> response2 = restClient.updateOntology(tenant, ontologyDto);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));

    String identifier = ontologyDto.getIdentifier();
    ResponseEntity<OntologyDto> response3 = restClient.getOntologyByIdentifier(tenant, identifier);
    OntologyDto outputDtos3 = response3.getBody();

    assertNotNull(outputDtos3);
    assertEquals(ontologyDto.getIdentifier(), outputDtos3.getIdentifier());
    assertEquals(ontologyDto.getName(), outputDtos3.getName());
    assertNotEquals(ontologyDto.getCreationDate(), outputDtos3.getCreationDate());
    assertEquals(ontologyDto.getStatus(), outputDtos3.getStatus());
    assertEquals(1, outputDtos3.getLifeCycles().size());
  }
}
