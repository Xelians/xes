/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.accesskey;

import static fr.xelians.esafe.organization.accesskey.AccessKeyClaimNames.ORGANIZATION_ID;
import static fr.xelians.esafe.organization.accesskey.AccessKeyClaimNames.USER_ID;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/*
 * @author Youcef Bouhaddouza
 */
public class AccessKeyGenerator {

  private static final String ACCESS_KEY_TYPE_BEARER = "Bearer";

  private final JwtEncoder encoder;

  private final AccessKeyProperties accessKeyProperties;

  public AccessKeyGenerator(JwtEncoder encoder, AccessKeyProperties accessKeyProperties) {
    this.encoder = encoder;
    this.accessKeyProperties = accessKeyProperties;
  }

  public AccessKeyDto generate(AccessKeyClaim accessKeyClaim) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .issuer(accessKeyProperties.getIssuerUri())
            .issuedAt(now)
            .subject(accessKeyClaim.getSubject())
            .claim(ORGANIZATION_ID, accessKeyClaim.getOrganizationId())
            .claim(USER_ID, accessKeyClaim.getUserId())
            .expiresAt(now.plus(accessKeyProperties.getTimeToLive().toDays(), ChronoUnit.DAYS))
            .build();

    String accessKey = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return new AccessKeyDto(
        accessKey, ACCESS_KEY_TYPE_BEARER, accessKeyProperties.getTimeToLive().getSeconds());
  }
}
