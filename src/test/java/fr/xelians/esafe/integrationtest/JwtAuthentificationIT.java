/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;

import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.authentication.dto.RefreshDto;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.dto.UserInfoDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class JwtAuthentificationIT extends BaseIT {

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
  void notAuthenticatedTest() {
    final UserDto userDto = DtoFactory.createUserDto();
    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.createUsers(userDto));

    // Client is not authenticated
    assertEquals(HttpStatus.UNAUTHORIZED, t1.getStatusCode(), t1.toString());
  }

  @Test
  void notAuthenticatedLogoutTest() {
    // logout with not authenticated client
    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.logout());
    assertEquals(HttpStatus.UNAUTHORIZED, t1.getStatusCode(), t1.toString());
  }

  @Test
  void authenticatedLogoutTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Logout
    ResponseEntity<Object> r2 = restClient.logout();
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    // Logout
    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.logout());
    assertEquals(HttpStatus.UNAUTHORIZED, t1.getStatusCode(), t1.toString());
  }

  @Test
  void authenticatedBadTenantLogoutTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Logout
    ResponseEntity<Object> r2 = restClient.logout();
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    HttpClientErrorException t2 =
        assertThrows(HttpClientErrorException.class, () -> restClient.logout());
    assertEquals(HttpStatus.UNAUTHORIZED, t2.getStatusCode(), t2.toString());
  }

  @Test
  void authenticatedWithRefreshNotExpiredTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    // Retrieve info
    AccessDto accessDto = r1.getBody();
    assertNotNull(accessDto);

    ResponseEntity<List<UserDto>> r2 = restClient.listUsers();
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    // Send refresh token too early
    RefreshDto refreshDto = new RefreshDto();
    refreshDto.setAccessToken(accessDto.getAccessToken());
    refreshDto.setRefreshToken(accessDto.getRefreshToken());

    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.refresh(refreshDto));
    assertEquals(HttpStatus.FORBIDDEN, t1.getStatusCode(), t1.toString());
  }

  @Test
  void authenticatedWithBadAccessTokenTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> response = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    AccessDto accessDto = response.getBody();
    assertNotNull(accessDto);
    String accessToken = accessDto.getAccessToken();
    String refreshToken = accessDto.getRefreshToken();

    // Wait for refresh to expire
    waitForRefreshTokenToExpire();

    // Send bad Access Token
    RefreshDto refreshDto1 = new RefreshDto();
    refreshDto1.setAccessToken("BadAccessToken");
    refreshDto1.setRefreshToken(accessDto.getRefreshToken());

    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.refresh(refreshDto1));
    assertEquals(HttpStatus.FORBIDDEN, t1.getStatusCode(), t1.toString());

    // Send good tokens (but too late)
    RefreshDto refreshDto2 = new RefreshDto();
    refreshDto2.setAccessToken(accessDto.getAccessToken());
    refreshDto2.setRefreshToken(accessDto.getRefreshToken());

    HttpClientErrorException t2 =
        assertThrows(HttpClientErrorException.class, () -> restClient.refresh(refreshDto2));
    assertEquals(HttpStatus.FORBIDDEN, t2.getStatusCode(), t2.toString());

    // Signin again
    ResponseEntity<AccessDto> r4 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    accessDto = r4.getBody();
    assertNotNull(accessDto);
    assertNotEquals(accessToken, accessDto.getAccessToken());
    assertNotEquals(refreshToken, accessDto.getRefreshToken());

    // Retrieve info
    ResponseEntity<List<UserDto>> r5 = restClient.listUsers();
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
  }

  @Test
  void authenticatedWithBadRefreshTokenTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> r1 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));
    AccessDto accessDto = r1.getBody();
    assertNotNull(accessDto);

    // Wait for refresh to expire
    waitForRefreshTokenToExpire();

    // Send bad refresh
    final RefreshDto refreshDto1 = new RefreshDto();
    refreshDto1.setAccessToken(accessDto.getAccessToken());
    refreshDto1.setRefreshToken("BadRefreshToken");

    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.refresh(refreshDto1));
    assertEquals(HttpStatus.FORBIDDEN, t1.getStatusCode(), t1.toString());

    // Send good refresh
    final RefreshDto refreshDto2 = new RefreshDto();
    refreshDto2.setAccessToken(accessDto.getAccessToken());
    refreshDto2.setRefreshToken(accessDto.getRefreshToken());

    ResponseEntity<AccessDto> r2 = restClient.refresh(refreshDto2);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
  }

  @Test
  void authenticatedWithRefreshTest() {
    // Client is authenticated
    ResponseEntity<AccessDto> response = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    AccessDto accessDto = response.getBody();
    assertNotNull(accessDto);

    // Wait for refresh
    waitForRefreshTokenToExpire();

    // Send good refresh tokens
    RefreshDto refreshDto = new RefreshDto();
    refreshDto.setAccessToken(accessDto.getAccessToken());
    refreshDto.setRefreshToken(accessDto.getRefreshToken());
    ResponseEntity<AccessDto> r2 = restClient.refresh(refreshDto);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));

    accessDto = r2.getBody();
    assertNotNull(accessDto);
    assertFalse(accessDto.getAccessToken().isEmpty());
    assertFalse(accessDto.getRefreshToken().isEmpty());

    // Get orga & user
    ResponseEntity<UserInfoDto> r5 = restClient.getMe();
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
    UserInfoDto userInfoDto = r5.getBody();
    assertNotNull(userInfoDto);
    assertEquals(loginDto.getUsername(), userInfoDto.getUserDto().getUsername());
    assertEquals(
        signupDto.getOrganizationDto().getIdentifier(),
        userInfoDto.getOrganizationDto().getIdentifier());

    // Retrieve info
    ResponseEntity<List<UserDto>> r6 = restClient.listUsers();
    assertEquals(HttpStatus.OK, r6.getStatusCode(), TestUtils.getBody(r6));
  }

  // Wait 5 sec max (configure settings in IT properties accordingly)
  private void waitForRefreshTokenToExpire() {
    for (int i = 0; i < 100; i++) {
      Utils.sleep(50);
      try {
        restClient.listUsers();
      } catch (HttpClientErrorException ex) {
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
          return;
        }
      }
    }
  }
}
