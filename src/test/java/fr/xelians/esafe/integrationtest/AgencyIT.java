/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.referential.dto.AgencyDto;
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

class AgencyIT extends BaseIT {

  private static final Path AGENCY1 = Paths.get(ItInit.AGENCY + "OK_agencies_init.csv");

  @BeforeAll
  void beforeAll() throws IOException {
    setup();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void createCsvAgenciesTest() throws IOException {
    Path dir = Paths.get(ItInit.AGENCY);
    for (Path path : TestUtils.listFiles(dir, "OK_", ".csv")) {
      ResponseEntity<List<AgencyDto>> response = restClient.createAgencies(nextTenant(), path);
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  @Test
  void doubleCreateCsvAgenciesTest() throws IOException {
    Long tenant = nextTenant();

    ResponseEntity<List<AgencyDto>> r1 = restClient.createAgencies(tenant, AGENCY1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<List<AgencyDto>> r2 = restClient.createAgencies(tenant, AGENCY1);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  }

  @Test
  void createBadCsvAgenciesTest() throws IOException {
    Path dir = Paths.get(ItInit.AGENCY);
    for (Path path : TestUtils.listFiles(dir, "KO_", ".csv")) {
      Long tenant = nextTenant();
      HttpClientErrorException thrown =
          assertThrows(
              HttpClientErrorException.class, () -> restClient.createAgencies(tenant, path));
      assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
    }
  }

  @Test
  void findCsvAgenciesTest() throws IOException {
    Long tenant = nextTenant();

    ResponseEntity<List<AgencyDto>> r1 = restClient.createAgencies(nextTenant(), AGENCY1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<String> r2 = restClient.getCsvAgencies(tenant);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  }

  @Test
  void getAgenciesTest() {
    Long tenant = nextTenant();

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AgencyDto>> response =
          restClient.createAgencies(tenant, DtoFactory.createAgencyDto(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }

    Map<String, Object> params = Map.of("sortby", "name", "sortdir", "desc");

    ResponseEntity<PageResult<AgencyDto>> response2 = restClient.getAgencies(tenant, params);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    PageResult<AgencyDto> dto = response2.getBody();

    assertEquals(0, dto.hits().offset());
    assertEquals(3, dto.results().size());
    assertEquals(3, dto.hits().size());
    assertEquals(20, dto.hits().limit());
    assertEquals(3, dto.hits().total());

    List<AgencyDto> ics = dto.results();
    assertEquals("NAME-3", ics.get(0).getName());
    assertEquals("NAME-2", ics.get(1).getName());
    assertEquals("NAME-1", ics.get(2).getName());
  }

  @Test
  void searchAgenciesTest() {
    Long tenant = nextTenant();

    String query =
        """
                 {
                   "$query": [
                      {
                         "$neq": { "identifier": "BAD_666" }
                     }
                   ],
                   "$filter": {
                       "$orderby": { "identifier": -1 }
                  },
                   "$projection": {}
                }
                """;

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<AgencyDto>> response =
          restClient.createAgencies(tenant, DtoFactory.createAgencyDto(i));
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchAgencies(tenant, query);
    SearchResult<JsonNode> outputDtos2 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos2);
    assertEquals(3, outputDtos2.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos2.hits().total(), TestUtils.getBody(r3));
  }
}
