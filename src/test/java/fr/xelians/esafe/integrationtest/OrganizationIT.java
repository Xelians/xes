/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.integrationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.UserInfoDto;
import fr.xelians.esafe.testcommon.TestUtils;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrganizationIT extends BaseIT {

  @BeforeAll
  void beforeAll() throws IOException {
    setup();
  }

  @Test
  void updateOrganizationTest() {

    // Get Organization
    ResponseEntity<UserInfoDto> r3 = restClient.getMe();
    UserInfoDto userInfoDto = r3.getBody();
    assertNotNull(userInfoDto);

    // Update Organization
    OrganizationDto organizationDto = userInfoDto.getOrganizationDto();
    organizationDto.setDescription("My Updated Description");
    ResponseEntity<OrganizationDto> r4 = restClient.updateOrganization(organizationDto);
    assertEquals(HttpStatus.OK, r4.getStatusCode(), TestUtils.getBody(r4));

    // Get Organization
    ResponseEntity<UserInfoDto> r5 = restClient.getMe();
    assertEquals(HttpStatus.OK, r5.getStatusCode(), TestUtils.getBody(r5));
    userInfoDto = r5.getBody();
    assertNotNull(userInfoDto);
    assertEquals(
        organizationDto.getDescription(), userInfoDto.getOrganizationDto().getDescription());
  }
}
