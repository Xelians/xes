/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static fr.xelians.esafe.common.constant.Api.*;
import static fr.xelians.esafe.common.constant.Header.X_APPLICATION_ID;
import static fr.xelians.esafe.common.constant.Header.X_TENANT_ID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.xelians.esafe.referential.dto.OntologyDto;
import fr.xelians.esafe.referential.service.AgencyService;
import fr.xelians.esafe.referential.service.IngestContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.referential.service.ProfileService;
import fr.xelians.esafe.referential.service.RuleService;
import fr.xelians.esafe.testcommon.DtoFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

// https://reflectoring.io/spring-boot-web-controller-test/
// https://reflectoring.io/bean-validation-with-spring-boot/
// https://dzone.com/articles/integration-testing-in-spring-boot-1

class ReferentialIT extends BaseIT {

  private static final Path ONTOLOGY = Paths.get(ItInit.ONTOLOGY + "OK_indexmap_1.json");
  private static final Path BAD_ONTOLOGY = Paths.get(ItInit.ONTOLOGY + "KO_indexmap_1.json");
  private static final String ONTOLOGIES_URL = ADMIN_EXTERNAL + V1 + ONTOLOGIES;

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private OntologyService ontologyService;
  @MockBean private AgencyService agencyService;
  @MockBean private IngestContractService ingestContractService;
  @MockBean private ProfileService profileService;
  @MockBean private RuleService ruleService;

  //    @Test
  void whenInputIsValid_thenReturnsStatus200() throws Exception {
    List<OntologyDto> ontologyDto = DtoFactory.createOntologyDtos(ONTOLOGY);
    String body = objectMapper.writeValueAsString(ontologyDto);
    mvc.perform(
            post(ONTOLOGIES_URL)
                .headers(createHeaders())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk());
  }

  //    @Test
  void whenInputIsInvalid_thenReturnsStatus400() throws Exception {
    List<OntologyDto> ontologyDto = DtoFactory.createOntologyDtos(BAD_ONTOLOGY);
    String body = objectMapper.writeValueAsString(ontologyDto);
    mvc.perform(
            post(ONTOLOGIES_URL)
                .headers(createHeaders())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest());
  }

  private HttpHeaders createHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(X_TENANT_ID, "100");
    headers.set(X_APPLICATION_ID, "10");
    return headers;
  }
}
