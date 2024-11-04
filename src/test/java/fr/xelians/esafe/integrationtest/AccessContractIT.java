/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.referential.dto.AccessContractDto;
import fr.xelians.esafe.referential.dto.AgencyDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class AccessContractIT extends BaseIT {

  private static final Path OK_AGENCY = Paths.get(ItInit.AGENCY + "OK_agencies_init.csv");

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void createAccessContractTest() throws IOException {
    Path dir = Paths.get(ItInit.ACCESS_CONTRACT);
    for (int i = 0; i < 4; i++) {
      for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".json")) {
        Long tenant = nextTenant();

        ResponseEntity<List<AgencyDto>> r1 = restClient.createCsvAgency(tenant, OK_AGENCY);
        assertEquals(HttpStatus.CREATED, r1.getStatusCode(), TestUtils.getBody(r1));

        ResponseEntity<List<AccessContractDto>> r2 = restClient.createAccessContract(tenant, path);
        assertEquals(HttpStatus.CREATED, r2.getStatusCode(), "path: " + path);

        List<AccessContractDto> acList = r2.getBody();
        assertNotNull(acList, "path: " + path);
        for (AccessContractDto ac : acList) {
          ResponseEntity<AccessContractDto> r3 =
              restClient.getAccessContractByIdentifier(tenant, ac.getIdentifier());
          assertEquals(HttpStatus.OK, r3.getStatusCode(), "path: " + path);
        }
      }
    }
  }

  @Test
  void createBadAccessContractTest() throws IOException {
    Path dir = Paths.get(ItInit.ACCESS_CONTRACT);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_", ".json")) {
      Long tenant = nextTenant();

      ResponseEntity<List<AgencyDto>> response = restClient.createCsvAgency(tenant, OK_AGENCY);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), "path: " + path);

      assertThrows(
          HttpClientErrorException.class, () -> restClient.createAccessContract(tenant, path));
    }
  }

  @Test
  void getAccessContractTest() {
    Long tenant = nextTenant();

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, DtoFactory.createAccessContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    Map<String, Object> params = Map.of("sortby", "name", "sortdir", "desc");
    ResponseEntity<PageResult<AccessContractDto>> response2 =
        restClient.getAccessContracts(tenant, params);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    PageResult<AccessContractDto> dto = response2.getBody();

    assertEquals(0, dto.hits().offset());
    assertEquals(3, dto.results().size());
    assertEquals(3, dto.hits().size());
    assertEquals(20, dto.hits().limit());
    assertEquals(3, dto.hits().total());

    List<AccessContractDto> acs = dto.results();
    assertEquals("NAME-3", acs.get(0).getName());
    assertEquals("NAME-2", acs.get(1).getName());
    assertEquals("NAME-1", acs.get(2).getName());

    for (int i = 0; i < 3; i++) {
      assertEquals(LocalDate.of(1969, Month.FEBRUARY, 27), acs.get(i).getActivationDate());
      assertEquals(LocalDate.of(2070, Month.DECEMBER, 1), acs.get(i).getDeactivationDate());
    }
  }

  @Test
  void searchAccessContractTest() {
    Long tenant = nextTenant();

    String query =
        """
           {
             "$query": [
                {
                "$and": [
                   {
                     "$match_phrase_prefix": { "Name": "NAME" }
                   },
                   {
                     "$match": { "Description": "ESCRIPT YOUPBABOUM"  }
                   },
                   {
                     "$or": [ { "$match_all": { "Description": "ESCR IPTI" } } , { "$eq": { "Description": "Gurps!!!!" } } ]
                   },
                   {
                     "$gte": { "ActivationDate": "1969-02-27" }
                   },
                   {
                     "$lte": { "ActivationDate": "1969-02-27" }
                   },
                   {
                     "$lt": { "ActivationDate": "2020-12-27" }
                   },
                   {
                     "$not": [ { "$gt": { "ActivationDate": "2020-12-27" } } ]
                   },
                   {
                     "$in": { "ActivationDate": ["1969-02-27", "1970-01-01"] }
                   },
                   {
                     "$nin": { "ActivationDate": ["1969-02-26", "1969-02-28", "1971-01-01"] }
                   },
                   {
                     "$eq": { "Status": "ACTIVE" }
                   },
                   {
                     "$neq": { "Status": "INACTIVE" }
                   }
                ]
               }
             ],
             "$filter": {
                 "$offset": 0,
                 "$limit": 2,
                 "$orderby": { "Identifier": -1 }
            },
             "$projection": {}
          }
          """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, DtoFactory.createAccessContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchAccessContracts(tenant, query);
    SearchResult<JsonNode> outputDtos3 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos3);
    assertEquals(2, outputDtos3.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos3.hits().total(), TestUtils.getBody(r3));
  }

  @Test
  void searchAccessContractWithEmptyInTest() {
    Long tenant = nextTenant();

    String query =
        """
               {
                 "$query": [
                    {
                    "$and": [
                       {
                         "$in": { "ActivationDate": [] }
                       },
                       {
                         "$nin": { "ActivationDate": [] }
                       }
                    ]
                   }
                 ],
                 "$projection": {}
              }
              """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, DtoFactory.createAccessContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchAccessContracts(tenant, query);
    SearchResult<JsonNode> outputDtos3 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos3);
    assertEquals(0, outputDtos3.results().size(), TestUtils.getBody(r3));
    assertEquals(0, outputDtos3.hits().total(), TestUtils.getBody(r3));
  }

  @Test
  void searchAccessContractWithEmptyQueryTest() {
    Long tenant = nextTenant();

    String query =
        """
                   {
                     "$query": [],
                     "$projection": {}
                  }
                  """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, DtoFactory.createAccessContractDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchAccessContracts(tenant, query);
    SearchResult<JsonNode> outputDtos3 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos3);
    assertEquals(3, outputDtos3.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos3.hits().total(), TestUtils.getBody(r3));
  }
}
