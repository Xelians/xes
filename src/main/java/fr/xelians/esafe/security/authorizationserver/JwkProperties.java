/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.security.authorizationserver;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/*
 * @author Youcef Bouhaddouza
 */
@Configuration
@ConfigurationProperties("authn.oauth2.jwk")
@Getter
@Setter
@Slf4j
class JwkProperties {

  private String keyId = "6d71b2ef-f26e-4c96-a416-4e717a081263";

  private RSAPublicKey publicKey;

  private RSAPrivateKey privateKey;

  private int keySize = 4048;

  public KeyPair getKeyPair() {
    if (publicKey == null || privateKey == null) {
      log.warn("Your are using in memory JWK. It is not recommended for production environments.");
      return generateInMemoryRsaKeys();
    }
    return new KeyPair(publicKey, privateKey);
  }

  private KeyPair generateInMemoryRsaKeys() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(keySize);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }
}
