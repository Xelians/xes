/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.vitamtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.integrationtest.SetupDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.service.sedav2.Sedav2Service;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

// @DirtiesContext is not needed because the context does not change between tests
@Slf4j
@ActiveProfiles("vitam")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ContextConfiguration(initializers = VitamInit.class)
public class VitamBaseIT {

  protected final Sedav2Service sedaService = Sedav2Service.getInstance();
  @LocalServerPort protected int port;
  protected RestClient restClient;

  protected SetupDto setup() {
    if (restClient == null) {
      restClient = new RestClient(port);
    }

    SignupDto inputDto = DtoFactory.createSignupDto();
    ResponseEntity<SignupDto> r1 = restClient.signup(inputDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    SignupDto signupDto = r1.getBody();
    assertNotNull(signupDto);
    Long tenant = signupDto.getTenantDto().getId();

    UserDto userDto = inputDto.getUserDto();
    LoginDto loginDto = new LoginDto(userDto.getUsername(), userDto.getPassword());
    ResponseEntity<AccessDto> r2 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    return new SetupDto(tenant, signupDto, userDto);
  }

  protected Long nextTenant() {
    TenantDto tenantDto = DtoFactory.createTenantDto();
    ResponseEntity<List<TenantDto>> response = restClient.createTenants(tenantDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    assertNotNull(response.getBody());
    return response.getBody().getFirst().getId();
  }
}
