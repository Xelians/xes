/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.organization.accesskey;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/*
 * @author Youcef Bouhaddouza
 */
@Configuration
@ConfigurationProperties("authn.access-key")
@Getter
@Setter
public class AccessKeyProperties {

  private Duration timeToLive = Duration.ofDays(10L);

  private String issuerUri = "self";

  private boolean persisted = true; // This must be true in production environments

  private RSAPublicKey publicKey;

  private RSAPrivateKey privateKey;
}
