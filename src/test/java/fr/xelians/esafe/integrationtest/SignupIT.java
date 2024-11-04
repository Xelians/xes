/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SignupIT extends BaseIT {

  private RestClient restClient;

  @BeforeAll
  void beforeAll() {
    restClient = new RestClient(port);
  }

  @Test
  void signupTest() {
    // Register organization
    SignupDto signupDto = DtoFactory.createSignupDto();
    ResponseEntity<SignupDto> response = restClient.signup(signupDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    SignupDto outputDto = response.getBody();

    assertNotNull(outputDto);
    assertEquals(
        signupDto.getOrganizationDto().getIdentifier(),
        outputDto.getOrganizationDto().getIdentifier());
    assertEquals(
        signupDto.getOrganizationDto().getName(), outputDto.getOrganizationDto().getName());
    assertEquals(signupDto.getUserDto().getUsername(), outputDto.getUserDto().getUsername());
    assertEquals(signupDto.getUserDto().getEmail(), outputDto.getUserDto().getEmail());
  }
}
