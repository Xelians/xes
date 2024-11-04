/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.xelians.esafe.organization.accesskey.AccessKeyDto;
import fr.xelians.esafe.organization.domain.Root;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.service.AccessKeyService;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import fr.xelians.sipg.service.sedav2.Sedav2Service;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Durations;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.shaded.org.awaitility.Awaitility;

// @DirtiesContext is not needed because the context does not change between tests
@Slf4j
@ActiveProfiles("it")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ContextConfiguration(initializers = ItInit.class)
public class BaseIT {

  @Autowired protected AccessKeyService accessKeyService;

  protected final Sedav2Service sedaService = Sedav2Service.getV22Instance();
  @LocalServerPort protected int port;

  protected RestClient restClient;
  private SetupDto setupDto;

  static {
    Awaitility.setDefaultPollDelay(Duration.ZERO);
    Awaitility.setDefaultPollInterval(Durations.ONE_HUNDRED_MILLISECONDS);
    Awaitility.setDefaultTimeout(Durations.FIVE_SECONDS);
  }

  protected synchronized SetupDto setup() {
    if (restClient == null) {
      restClient = new RestClient(port);

      SignupDto inputDto = DtoFactory.createSignupDto();
      ResponseEntity<SignupDto> r1 = restClient.signup(inputDto);
      assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

      SignupDto signupDto = r1.getBody();
      assertNotNull(signupDto);

      String orgaIdentifier = signupDto.getOrganizationDto().getIdentifier();
      String userIdentifier = signupDto.getUserDto().getIdentifier();
      String accessKey = accessKeyService.createToken(orgaIdentifier, userIdentifier).accessKey();
      restClient.signIn(accessKey);

      UserDto userDto = inputDto.getUserDto();
      Long tenant = signupDto.getTenantDto().getId();
      setupDto = new SetupDto(tenant, signupDto, userDto);
    }

    return setupDto;
  }

  protected Long nextTenant() {
    TenantDto tenantDto = DtoFactory.createTenantDto();
    ResponseEntity<List<TenantDto>> response = restClient.createTenants(tenantDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    assertNotNull(response.getBody());
    return response.getBody().getFirst().getId();
  }

  protected UserDto signInAsRootAdmin() {
    AccessKeyDto token = accessKeyService.createToken(Root.ORGA_IDENTIFIER, Root.USER_IDENTIFIER);
    restClient.signIn(token.accessKey());

    UserDto user = restClient.getUser(Root.USER_IDENTIFIER).getBody();
    assertNotNull(user);
    user.setPassword("***"); // TODO : why update user without password generate NPE
    return user;
  }
}
