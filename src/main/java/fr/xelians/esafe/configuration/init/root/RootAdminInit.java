/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration.init.root;

import static fr.xelians.esafe.organization.domain.Role.GlobalRole.ROLE_ROOT_ADMIN;

import fr.xelians.esafe.organization.accesskey.AccessKeyDto;
import fr.xelians.esafe.organization.domain.Root;
import fr.xelians.esafe.organization.dto.OrganizationDto;
import fr.xelians.esafe.organization.dto.SignupDto;
import fr.xelians.esafe.organization.dto.TenantDto;
import fr.xelians.esafe.organization.dto.UserDto;
import fr.xelians.esafe.organization.entity.UserDb;
import fr.xelians.esafe.organization.repository.UserRepository;
import fr.xelians.esafe.organization.service.AccessKeyService;
import fr.xelians.esafe.organization.service.SignupService;
import fr.xelians.esafe.referential.domain.Status;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/*
 * @author Julien Cornille
 */
@Slf4j
@Component
@AllArgsConstructor
public class RootAdminInit implements ApplicationListener<ApplicationReadyEvent> {

  private final Validator validator;
  private final RootAdminProperties rootAdminProperties;
  private final AccessKeyService accessKeyService;
  private final SignupService signupService;
  private final UserRepository userRepository;

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("Starting root admin init...");

    if (rootAdminProperties == null) {
      log.info("Skipping RootAdmin initialization - RootAdmin is not defined");
      return;
    }

    UserDto rootAdminDto = createUserDto(rootAdminProperties);
    Set<ConstraintViolation<UserDto>> cv = validator.validate(rootAdminDto);
    if (!cv.isEmpty()) {
      log.warn("Skipping RootAdmin initialization - RootAdmin is not properly defined: {}", cv);
      return;
    }

    RootAdminProperties.Tenant tenant = rootAdminProperties.getTenant();
    if (tenant == null) {
      log.warn("Skipping RootAdmin initialization - RootAdmin.Tenant is not defined");
      return;
    }

    if (CollectionUtils.isEmpty(tenant.getStorageOffers())) {
      log.warn(
          "Skipping RootAdmin initialization - RootAdmin.Tenant.StrorageOffers is not defined");
      return;
    }

    List<UserDb> rootAdmin = userRepository.findByOrganizationIdentifier(Root.ORGA_IDENTIFIER);
    if (rootAdmin.isEmpty()) {

      log.info("Initializing RootAdmin with username {}", rootAdminDto.getUsername());

      TenantDto rootTenantDto = createTenantDto(tenant);
      OrganizationDto rootOrgaDto = createOrganisationDto();
      SignupDto rootSignupDto = createSignupDto(rootAdminDto, rootTenantDto, rootOrgaDto);
      signupService.createRoot(rootSignupDto);

      if (rootAdminProperties.isInitAccessKey()) {
        AccessKeyDto token =
            accessKeyService.createToken(Root.ORGA_IDENTIFIER, Root.USER_IDENTIFIER);
        log.info("Root admin access key={}", token);
      }
      log.info("RootAdmin initialization completed for username {}.", rootAdminDto.getUsername());
    }
  }

  // Create the root admin user with ROLE_ROOT_ADMIN roles that gives all rights
  private UserDto createUserDto(RootAdminProperties rootAdminProperties) {
    UserDto userDto = new UserDto();
    userDto.setIdentifier(Root.USER_IDENTIFIER);
    userDto.setName("ROOT");
    userDto.setUsername(rootAdminProperties.getUsername());
    userDto.setEmail(rootAdminProperties.getEmail());
    userDto.setPassword(rootAdminProperties.getPassword());
    userDto.setFirstName("ROOT");
    userDto.setLastName("ADMIN");
    userDto.setGlobalRoles(new ArrayList<>(List.of(ROLE_ROOT_ADMIN)));
    userDto.setStatus(Status.ACTIVE);
    userDto.setOrganizationIdentifier(Root.ORGA_IDENTIFIER);
    return userDto;
  }

  private SignupDto createSignupDto(UserDto userDto, TenantDto tenantDto, OrganizationDto orgaDto) {
    final var signupDto = new SignupDto();
    signupDto.setOrganizationDto(orgaDto);
    signupDto.setTenantDto(tenantDto);
    signupDto.setUserDto(userDto);
    return signupDto;
  }

  // Create the root tenant
  private TenantDto createTenantDto(RootAdminProperties.Tenant tenant) {
    TenantDto tenantDto = new TenantDto();
    tenantDto.setName("ROOT");
    tenantDto.setDescription("ROOT");
    tenantDto.setStatus(Status.ACTIVE);
    tenantDto.setStorageOffers(tenant.getStorageOffers());
    tenantDto.setEncrypted(tenant.getEncrypted());
    tenantDto.setOrganizationIdentifier(Root.ORGA_IDENTIFIER);
    return tenantDto;
  }

  // Create the root organization
  private OrganizationDto createOrganisationDto() {
    OrganizationDto organizationDto = new OrganizationDto();
    organizationDto.setIdentifier(Root.ORGA_IDENTIFIER);
    organizationDto.setName("ROOT");
    organizationDto.setStatus(Status.ACTIVE);
    return organizationDto;
  }
}
