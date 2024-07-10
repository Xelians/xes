/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.*;

import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class UserIT extends BaseIT {

  private SignupDto signupDto;

  @BeforeAll
  void beforeAll() throws IOException {
    SetupDto setupDto = setup();
    signupDto = setupDto.signupDto();
  }

  @Test
  void createUserTest() {
    UserDto[] users1 = new UserDto[3];
    for (int j = 0; j < users1.length; j++) {
      users1[j] = DtoFactory.createUserDto();
    }

    ResponseEntity<List<UserDto>> response = restClient.createUsers(users1);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    List<UserDto> users2 = response.getBody();
    assertNotNull(users2);
    assertEquals(users1[0].getEmail(), users2.getFirst().getEmail());
    assertEquals(users1[0].getUsername(), users2.getFirst().getUsername());
    assertEquals(users1[0].getFirstName(), users2.getFirst().getFirstName());
    assertEquals(users1[0].getLastName(), users2.getFirst().getLastName());
    assertEquals(users1[0].getDescription(), users2.getFirst().getDescription());
  }

  @Test
  void updateUserTest() {
    UserDto user1 = DtoFactory.createUserDto();
    ResponseEntity<List<UserDto>> r1 = restClient.createUsers(user1);
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));
    List<UserDto> users = r1.getBody();
    assertNotNull(users);
    UserDto user2 = users.getFirst();
    assertEquals(user1.getEmail(), user2.getEmail());

    UserDto user3 = DtoFactory.createUserDto(user1.getIdentifier());
    user3.setName(user1.getName() + "bis");
    ResponseEntity<UserDto> r2 = restClient.updateUser(user3);
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    UserDto user4 = r2.getBody();
    assertNotNull(user4);
    assertEquals(user1.getEmail(), user4.getEmail());
    assertEquals(user1.getName() + "bis", user4.getName());
    assertEquals(1, user4.getLifeCycles().size());

    UserDto user5 = DtoFactory.createUserDto(user1.getIdentifier());
    user5.setName(user1.getName() + "ter");
    ResponseEntity<UserDto> r3 = restClient.updateUser(user5);
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    UserDto user6 = r3.getBody();
    assertNotNull(user6);
    assertEquals(user1.getEmail(), user6.getEmail());
    assertEquals(user1.getName() + "ter", user6.getName());
    assertEquals(2, user6.getLifeCycles().size());

    ResponseEntity<UserDto> r4 = restClient.getUser(user1.getIdentifier());
    UserDto user7 = r4.getBody();
    assertNotNull(user7);
    assertEquals(user1.getIdentifier(), user7.getIdentifier(), TestUtils.getBody(r2));
    assertEquals(user1.getEmail(), user7.getEmail());
    assertEquals(user1.getName() + "ter", user7.getName());
    assertEquals(2, user7.getLifeCycles().size());
  }

  @Test
  void getUserTest() {

    UserDto user1 = DtoFactory.createUserDto("ABCDEFGHIJKL2345");

    ResponseEntity<List<UserDto>> response = restClient.createUsers(user1);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    List<UserDto> outputDtos = response.getBody();
    assertNotNull(outputDtos);
    UserDto outputDto = outputDtos.getFirst();
    assertEquals(user1.getEmail(), outputDto.getEmail());

    ResponseEntity<UserDto> response2 = restClient.getUser(outputDto.getIdentifier());
    UserDto user2 = response2.getBody();

    assertNotNull(user2);
    assertEquals(user1.getIdentifier(), user2.getIdentifier());
    assertEquals(user1.getName(), user2.getName());
    assertEquals(user1.getEmail(), user2.getEmail());
    assertEquals(user1.getFirstName(), user2.getFirstName());
    assertEquals(user1.getLastName(), user2.getLastName());
  }

  @Test
  void listUserTest() {
    ResponseEntity<List<UserDto>> response = restClient.listUsers();
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));
    List<UserDto> users = response.getBody();

    assertNotNull(users);
    assertTrue(
        users.stream()
            .map(UserDto::getEmail)
            .anyMatch(s -> s.equals(signupDto.getUserDto().getEmail())));
  }
}
