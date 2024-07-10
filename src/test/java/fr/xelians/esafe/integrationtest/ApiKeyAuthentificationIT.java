/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;

import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class ApiKeyAuthentificationIT extends BaseIT {

  private LoginDto loginDto;
  private SignupDto signupDto;
  private RestClient restClient;

  @BeforeAll
  void beforeAll() {
    restClient = new RestClient(port, false);

    SignupDto inputDto = DtoFactory.createSignupDto();
    ResponseEntity<SignupDto> response = restClient.signup(inputDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    signupDto = response.getBody();
    assertNotNull(signupDto);

    loginDto =
        new LoginDto(signupDto.getUserDto().getUsername(), inputDto.getUserDto().getPassword());
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void should_call_api_with_api_key() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Retrieve info
    AccessDto accessDto = r1.getBody();
    assertNotNull(accessDto);

    restClient.setUseApiKey(true);
    restClient.setApiKey("apikey-%d".formatted(signupDto.getTenantDto().getId()));
    restClient.setAccessToken(null);
    ResponseEntity<List<UserDto>> r2 = restClient.listUsers();
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    restClient.setUseApiKey(false);
  }

  @Test
  @Disabled("Fix when API return 401 instead of 500")
  void should_have_401_when_call_with_bad_api_key() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Retrieve info
    AccessDto accessDto = r1.getBody();
    assertNotNull(accessDto);

    restClient.setUseApiKey(true);
    restClient.setApiKey("badapikey");
    restClient.setAccessToken(null);
    ResponseEntity<List<UserDto>> r2 = restClient.listUsers();
    assertEquals(HttpStatus.UNAUTHORIZED, r2.getStatusCode(), TestUtils.getBody(r2));
    restClient.setUseApiKey(false);
  }
}
