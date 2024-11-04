/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.accesskey;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/*
 * @author Youcef Bouhaddouza
 */
@Configuration
class AccessKeyConfig {
  @Bean
  AccessKeyGenerator accessKeyGenerator(
      @Qualifier("accessKeyEncoder") JwtEncoder encoder, AccessKeyProperties accessKeyProperties) {
    return new AccessKeyGenerator(encoder, accessKeyProperties);
  }

  @Bean
  JwtEncoder accessKeyEncoder(AccessKeyProperties accessKeyProperties) {
    JWK jwk =
        new RSAKey.Builder(accessKeyProperties.getPublicKey())
            .privateKey(accessKeyProperties.getPrivateKey())
            .keyID(UUID.randomUUID().toString())
            .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
  }
}
