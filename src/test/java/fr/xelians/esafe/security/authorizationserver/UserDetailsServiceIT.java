package fr.xelians.esafe.security.authorizationserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import fr.xelians.esafe.integrationtest.BaseIT;
import fr.xelians.esafe.organization.dto.UserDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UserDetailsServiceIT extends BaseIT {

  private static final String NOT_EXISTING_USERNAME = "not-existing-user";

  private UserDto createdUser;

  @Autowired private UserDetailsService userDetailsService;

  @BeforeAll
  void beforeAll() {
    createdUser = setup().userDto();
  }

  @Test
  void loadUserByUsernameWhenUserNotFoundThenThrowException() {
    Throwable thrown =
        catchThrowable(() -> userDetailsService.loadUserByUsername(NOT_EXISTING_USERNAME));

    assertThat(thrown)
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage(NOT_EXISTING_USERNAME + " not found");
  }

  @Test
  void loadUserByUsernameWhenUserFoundThenReturnIt() {
    UserDetails userDetails = userDetailsService.loadUserByUsername(createdUser.getUsername());

    assertThat(userDetails).isNotNull();
    assertThat(userDetails)
        .extracting(UserDetails::getUsername)
        .isEqualTo(createdUser.getUsername());
  }
}
