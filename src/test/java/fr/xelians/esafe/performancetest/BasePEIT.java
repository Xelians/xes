/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.performancetest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

// @DirtiesContext is not needed because the context does not change between tests
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ExtendWith(PeInit.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("pe")
class BasePEIT {

  @LocalServerPort protected int port;

  protected RestClient restClient;
  protected long tenant;
  protected SignupDto signupDto;
  protected UserDto userDto;

  protected void signupSignin() {
    restClient = new RestClient(port);

    SignupDto inputDto = DtoFactory.createSignupDto();
    ResponseEntity<SignupDto> r1 = restClient.signup(inputDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode());

    signupDto = r1.getBody();
    assertNotNull(signupDto);

    userDto = signupDto.getUserDto();
    LoginDto loginDto = new LoginDto(userDto.getUsername(), userDto.getPassword());
    ResponseEntity<AccessDto> r2 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r2.getStatusCode());

    tenant = nextTenant();
  }

  @SuppressWarnings("unchecked")
  protected long nextTenant() {
    TenantDto tenantDto = DtoFactory.createTenantDto("FS:FS01");
    ResponseEntity<?> response = restClient.createTenants(tenantDto);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    tenantDto = ((List<TenantDto>) response.getBody()).get(0);
    tenant = tenantDto.getId();
    return tenant;
  }
}
