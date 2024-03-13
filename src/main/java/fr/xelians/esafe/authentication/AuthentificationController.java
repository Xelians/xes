/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.authentication;

import static fr.xelians.esafe.common.constant.Api.LOGOUT;
import static fr.xelians.esafe.common.constant.Api.REFRESH;
import static fr.xelians.esafe.common.constant.Api.SIGNIN;
import static fr.xelians.esafe.common.constant.Api.V1;
import static fr.xelians.esafe.organization.domain.role.RoleName.ROLE_ADMIN;

import fr.xelians.esafe.authentication.domain.AuthUserDetails;
import fr.xelians.esafe.authentication.dto.AccessDto;
import fr.xelians.esafe.authentication.dto.LoginDto;
import fr.xelians.esafe.authentication.dto.RefreshDto;
import fr.xelians.esafe.authentication.service.AuthenticationService;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class AuthentificationController {

  private final AuthenticationService authenticationService;

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(V1 + SIGNIN)
  public AccessDto signinUser(@Valid @RequestBody LoginDto loginDto) {
    return authenticationService.signin(loginDto.getUsername(), loginDto.getPassword());
  }

  @PostMapping(V1 + REFRESH)
  public ResponseEntity<AccessDto> refresh(@Valid @RequestBody RefreshDto request) {
    String atoken = request.getAccessToken();
    String rtoken = request.getRefreshToken();
    AccessDto accessDto = authenticationService.refreshToken(atoken, rtoken);
    return ResponseEntity.ok().body(accessDto);
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(V1 + LOGOUT)
  @Secured(ROLE_ADMIN)
  public void logoutUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BadRequestException(
          "Logout user failed", "Failed to logout because user is not authenticated");
    }
    AuthUserDetails user = (AuthUserDetails) authentication.getPrincipal();
    authenticationService.logout(user.getId());
  }
}
