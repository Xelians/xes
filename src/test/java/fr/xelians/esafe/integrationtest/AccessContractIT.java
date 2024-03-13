/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class AccessContractIT extends BaseIT {

  private static final Path OK_AGENCY = Paths.get(ItInit.AGENCY + "OK_agencies_init.csv");

  @BeforeAll
  void beforeAll() throws IOException {
    setup();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void createAccessContractTest() throws IOException {
    Path dir = Paths.get(ItInit.ACCESSCONTRACT);
    for (int i = 0; i < 4; i++) {
      for (Path path : TestUtils.listFiles(dir, "OK_", ".json")) {
        Long tenant = nextTenant();

        ResponseEntity<List<AgencyDto>> r1 = restClient.createAgencies(tenant, OK_AGENCY);
        assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

        ResponseEntity<List<AccessContractDto>> r2 = restClient.createAccessContract(tenant, path);
        assertEquals(HttpStatus.OK, r2.getStatusCode(), "path: " + path);

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
    Path dir = Paths.get(ItInit.ACCESSCONTRACT);
    for (Path path : TestUtils.listFiles(dir, "KO_", ".json")) {
      Long tenant = nextTenant();

      ResponseEntity<List<AgencyDto>> response = restClient.createAgencies(tenant, OK_AGENCY);
      assertEquals(HttpStatus.OK, response.getStatusCode(), "path: " + path);

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
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
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
                     "$match_phrase_prefix": { "name": "NAME" }
                   },
                   {
                     "$match": { "description": "ESCRIPT YOUPBABOUM"  }
                   },
                   {
                     "$or": [ { "$match_all": { "description": "ESCR IPTI" } } , { "$eq": { "description": "Gurps!!!!" } } ]
                   },
                   {
                     "$gte": { "activationDate": "1969-02-27" }
                   },
                   {
                     "$lte": { "activationDate": "1969-02-27" }
                   },
                   {
                     "$lt": { "activationDate": "2020-12-27" }
                   },
                   {
                     "$not": [ { "$gt": { "activationDate": "2020-12-27" } } ]
                   },
                   {
                     "$in": { "activationDate": ["1969-02-27", "1970-01-01"] }
                   },
                   {
                     "$nin": { "activationDate": ["1969-02-26", "1969-02-28", "1971-01-01"] }
                   },
                   {
                     "$eq": { "status": "ACTIVE" }
                   },
                   {
                     "$neq": { "status": "INACTIVE" }
                   }
                ]
               }
             ],
             "$filter": {
                 "$offset": 0,
                 "$limit": 2,
                 "$orderby": { "identifier": -1 }
            },
             "$projection": {}
          }
          """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AccessContractDto>> response =
          restClient.createAccessContract(tenant, DtoFactory.createAccessContractDto(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchAccessContracts(tenant, query);
    SearchResult<JsonNode> outputDtos3 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos3);
    assertEquals(2, outputDtos3.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos3.hits().total(), TestUtils.getBody(r3));
  }
}
