/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.service;

import fr.xelians.esafe.organization.accesskey.AccessKeyClaim;
import fr.xelians.esafe.organization.accesskey.AccessKeyDto;
import fr.xelians.esafe.organization.accesskey.AccessKeyGenerator;
import fr.xelians.esafe.organization.accesskey.AccessKeyProperties;
import fr.xelians.esafe.organization.entity.UserDb;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Emmanuel Deviller
 */
@Service
@AllArgsConstructor
public class AccessKeyService {

  private final UserService userService;

  private final AccessKeyGenerator accessKeyGenerator;

  private final PasswordEncoder passwordEncoder;

  private final AccessKeyProperties accessKeyProperties;

  @Transactional
  public AccessKeyDto createToken(String organizationIdentifier, String userIdentifier) {
    UserDb userDb = userService.getUserDbByIdentifier(organizationIdentifier, userIdentifier);
    AccessKeyDto accessKeyDto = generateToken(userDb);
    if (accessKeyProperties.isPersisted()) {
      userDb.setAccessKey(passwordEncoder.encode(accessKeyDto.accessKey()));
    }
    return accessKeyDto;
  }

  private AccessKeyDto generateToken(UserDb userDb) {
    return accessKeyGenerator.generate(mapToAccessKeyClaim(userDb));
  }

  private static AccessKeyClaim mapToAccessKeyClaim(UserDb userDb) {
    return AccessKeyClaim.builder()
        .subject(userDb.getUsername())
        .organizationId(userDb.getOrganization().getIdentifier())
        .userId(userDb.getIdentifier())
        .build();
  }
}
