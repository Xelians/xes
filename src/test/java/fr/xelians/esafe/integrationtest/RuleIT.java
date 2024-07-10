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
import fr.xelians.esafe.referential.dto.RuleDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class RuleIT extends BaseIT {

  private static final Path RULE1 = Paths.get(ItInit.RULE + "OK_regles_CSV.csv");

  @BeforeAll
  void beforeAll() throws IOException {
    setup();
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void createCsvRulesTest() throws IOException {
    Path dir = Paths.get(ItInit.RULE);
    for (Path path : TestUtils.listFiles(dir, "OK_", ".csv")) {
      ResponseEntity<List<RuleDto>> response = restClient.createCsvRule(nextTenant(), path);
      assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  @Test
  void createBadCsvRulesTest() throws IOException {
    Path dir = Paths.get(ItInit.RULE);
    for (Path path : TestUtils.listFiles(dir, "KO_", ".csv")) {
      Long tenant = nextTenant();
      HttpClientErrorException thrown =
          assertThrows(
              HttpClientErrorException.class, () -> restClient.createCsvRule(tenant, path));
      assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
    }
  }

  @Test
  void createRuleTest() {
    Long tenant = nextTenant();
    RuleDto ruleDto = DtoFactory.createAppraisalRule(1);
    ResponseEntity<List<RuleDto>> r1 = restClient.createRule(tenant, ruleDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<RuleDto> r2 = restClient.getRuleByIdentifier(tenant, ruleDto.getIdentifier());
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    RuleDto ruleDto2 = r2.getBody();
    assertNotNull(ruleDto2);
    assertEquals(ruleDto.getIdentifier(), ruleDto2.getIdentifier(), TestUtils.getBody(r2));
    assertEquals(ruleDto.getName(), ruleDto2.getName(), TestUtils.getBody(r2));

    ResponseEntity<PageResult<RuleDto>> r3 = restClient.getRules(tenant, Collections.emptyMap());
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
  }

  @Test
  void findCsvRulesTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, RULE1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<String> r2 = restClient.getCsvRules(tenant);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  }

  @Test
  void should_reimport_rules_csv_should_erase_and_recreate_rules() throws IOException {
    var tenant = nextTenant();
    Path path = Paths.get(ItInit.RULE + "OK_regles_CSV.csv");

    ResponseEntity<List<RuleDto>> response1 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.OK, response1.getStatusCode(), TestUtils.getBody(response1));

    ResponseEntity<List<RuleDto>> response2 = restClient.createCsvRule(tenant, path);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
  }

  @Test
  void searchRulesTest() throws IOException {
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
                   "$offset": 10,
                   "$limit": 12,
                   "$orderby": { "Identifier": 1 }
              },
               "$projection": {"$fields": { "Identifier": 1, "LastUpdate": 1, "Status": 1 }}
            }
            """;

    ResponseEntity<List<RuleDto>> r1 = restClient.createCsvRule(tenant, RULE1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchRules(tenant, query);
    SearchResult<JsonNode> outputDtos2 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos2);
    assertEquals(12, outputDtos2.results().size(), TestUtils.getBody(r3));
    assertEquals(62, outputDtos2.hits().total(), TestUtils.getBody(r3));
  }
}
