/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.search.search.SearchResult;
import fr.xelians.esafe.common.utils.PageResult;
import fr.xelians.esafe.referential.dto.ProfileDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
class ProfileIT extends BaseIT {

  private static final Path OK_PROFILE = Paths.get(ItInit.PROFILE + "OK_profil_mail.json");
  private static final Path OK_RNG = Paths.get(ItInit.PROFILE + "OK_profil_mail.rng");

  @BeforeAll
  void beforeAll() {
    setup();
  }

  @Test
  void createProfileTest() throws IOException {
    Path dir = Paths.get(ItInit.PROFILE);
    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".json")) {
      ResponseEntity<List<ProfileDto>> response = restClient.createProfile(nextTenant(), path);
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }
  }

  @Test
  void createBadProfileTest() throws IOException {
    Path dir = Paths.get(ItInit.PROFILE);
    for (Path path : TestUtils.filenamesStartWith(dir, "KO_", ".json")) {
      log.info("Test files ={}", path);
      Long tenant = nextTenant();
      HttpClientErrorException thrown =
          assertThrows(
              HttpClientErrorException.class, () -> restClient.createProfile(tenant, path));
      assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode(), thrown.toString());
    }
  }

  @Test
  void updateProfileTest() {
    Long tenant = nextTenant();

    ProfileDto profileDto = DtoFactory.createProfileDto(1);
    String identifier = profileDto.getIdentifier();
    String description = profileDto.getDescription();

    ProfileDto profileDto2 = DtoFactory.createProfileDto(2);

    ResponseEntity<List<ProfileDto>> response1 =
        restClient.createProfile(tenant, profileDto, profileDto2);
    assertEquals(HttpStatus.CREATED, response1.getStatusCode(), TestUtils.getBody(response1));
    assertNotNull(response1.getBody());
    assertEquals(
        description, response1.getBody().getFirst().getDescription(), TestUtils.getBody(response1));

    profileDto.setDescription(profileDto.getDescription() + " updated");
    ResponseEntity<ProfileDto> response2 = restClient.updateProfile(tenant, profileDto, identifier);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    assertEquals(
        profileDto.getDescription(),
        response2.getBody().getDescription(),
        TestUtils.getBody(response2));
    assertNotEquals(
        description, response2.getBody().getDescription(), TestUtils.getBody(response2));

    profileDto.setDescription(profileDto.getDescription() + " updated");
    ResponseEntity<ProfileDto> response3 = restClient.updateProfile(tenant, profileDto, identifier);
    assertEquals(HttpStatus.OK, response3.getStatusCode(), TestUtils.getBody(response3));
  }

  @Test
  void updateProfileRngTest() throws IOException {
    Path dir = Paths.get(ItInit.PROFILE);

    Long tenant = nextTenant();
    ResponseEntity<List<ProfileDto>> response = restClient.createProfile(tenant, OK_PROFILE);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    assertNotNull(response.getBody());
    ProfileDto profile = response.getBody().getFirst();

    for (Path path : TestUtils.filenamesStartWith(dir, "OK_", ".rng")) {
      ResponseEntity<Void> r =
          restClient.updateBinaryProfile(tenant, path, profile.getIdentifier());
      assertEquals(HttpStatus.OK, r.getStatusCode(), TestUtils.getBody(r));
    }
  }

  @Test
  void downloadProfileRngTest() throws IOException {
    Long tenant = nextTenant();
    ResponseEntity<List<ProfileDto>> response = restClient.createProfile(tenant, OK_PROFILE);
    assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    assertNotNull(response.getBody());
    ProfileDto profileDto = response.getBody().getFirst();

    byte[] oriBytes = Files.readAllBytes(OK_RNG);
    ResponseEntity<Void> r1 =
        restClient.updateBinaryProfile(tenant, OK_RNG, profileDto.getIdentifier());
    assertEquals(HttpStatus.OK, r1.getStatusCode(), TestUtils.getBody(r1));

    ResponseEntity<ProfileDto> r3 =
        restClient.getProfileByIdentifier(tenant, profileDto.getIdentifier());
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));
    profileDto = r3.getBody();
    assertNotNull(profileDto);
    assertEquals("profilrng_mail.rng", profileDto.getPath(), TestUtils.getBody(r3));

    ResponseEntity<byte[]> r2 = restClient.getBinaryProfile(tenant, profileDto.getIdentifier());
    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    byte[] newBytes = r2.getBody();

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    assertArrayEquals(oriBytes, newBytes);
  }

  @Test
  void getProfileTest() {
    Long tenant = nextTenant();

    for (int i = 1; i <= 3; i++) {
      ResponseEntity<List<ProfileDto>> response =
          restClient.createProfile(tenant, DtoFactory.createProfileDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    Map<String, Object> params = Map.of("sortby", "name", "sortdir", "desc");
    ResponseEntity<PageResult<ProfileDto>> response2 = restClient.getProfiles(tenant, params);
    assertEquals(HttpStatus.OK, response2.getStatusCode(), TestUtils.getBody(response2));
    assertNotNull(response2.getBody());
    PageResult<ProfileDto> dto = response2.getBody();

    assertEquals(0, dto.hits().offset());
    assertEquals(3, dto.results().size());
    assertEquals(3, dto.hits().size());
    assertEquals(20, dto.hits().limit());
    assertEquals(3, dto.hits().total());

    List<ProfileDto> ics = dto.results();
    assertEquals("NAME-3", ics.getFirst().getName());
    assertEquals("NAME-2", ics.get(1).getName());
    assertEquals("NAME-1", ics.get(2).getName());
  }

  @Test
  void searchProfilesTest() {
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
      ResponseEntity<List<ProfileDto>> response =
          restClient.createProfile(tenant, DtoFactory.createProfileDto(i));
      assertEquals(HttpStatus.CREATED, response.getStatusCode(), TestUtils.getBody(response));
    }

    ResponseEntity<SearchResult<JsonNode>> r3 = restClient.searchProfiles(tenant, query);
    SearchResult<JsonNode> outputDtos2 = r3.getBody();
    assertEquals(HttpStatus.OK, r3.getStatusCode(), TestUtils.getBody(r3));

    assertNotNull(outputDtos2);
    assertEquals(3, outputDtos2.results().size(), TestUtils.getBody(r3));
    assertEquals(3, outputDtos2.hits().total(), TestUtils.getBody(r3));
  }
}
