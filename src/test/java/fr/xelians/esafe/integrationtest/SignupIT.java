/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.testcommon.DtoFactory;
import fr.xelians.esafe.testcommon.RestClient;
import fr.xelians.esafe.testcommon.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

class SignupIT extends BaseIT {

  private RestClient restClient;

  @BeforeAll
  void beforeAll() {
    restClient = new RestClient(port);
  }

  @BeforeEach
  void beforeEach() {}

  @Test
  void signupTest() {
    // Register organization
    SignupDto signupDto = DtoFactory.createSignupDto();
    ResponseEntity<Void> response = restClient.signupRegister(signupDto);
    assertEquals(HttpStatus.OK, response.getStatusCode(), TestUtils.getBody(response));

    // Create new organization
    String key = signupDto.getOrganizationDto().getIdentifier();
    ResponseEntity<SignupDto> r2 = restClient.signupCreate(key);

    assertEquals(HttpStatus.OK, r2.getStatusCode(), TestUtils.getBody(r2));
    SignupDto outputDto = r2.getBody();

    assertNotNull(outputDto);
    assertEquals(
        signupDto.getOrganizationDto().getIdentifier(),
        outputDto.getOrganizationDto().getIdentifier());
    assertEquals(
        signupDto.getOrganizationDto().getName(), outputDto.getOrganizationDto().getName());
    assertEquals(signupDto.getUserDto().getUsername(), outputDto.getUserDto().getUsername());
    assertEquals(signupDto.getUserDto().getEmail(), outputDto.getUserDto().getEmail());

    // Login
    LoginDto loginDto = new LoginDto();
    loginDto.setUsername(signupDto.getUserDto().getUsername());
    loginDto.setPassword("VeryBadPassword");

    HttpClientErrorException t1 =
        assertThrows(HttpClientErrorException.class, () -> restClient.signin(loginDto));
    assertEquals(HttpStatus.UNAUTHORIZED, t1.getStatusCode(), t1.toString());

    loginDto.setPassword(signupDto.getUserDto().getPassword());

    ResponseEntity<AccessDto> r4 = restClient.signin(loginDto);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));
    AccessDto accessDto = r4.getBody();

    assertNotNull(accessDto);
  }
}
