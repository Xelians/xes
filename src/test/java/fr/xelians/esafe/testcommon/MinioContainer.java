/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

public class MinioContainer extends GenericContainer<MinioContainer> {

  private static final int DEFAULT_PORT = 9000;
  private static final String DEFAULT_IMAGE = "minio/minio";
  private static final String DEFAULT_TAG = "latest";

  private static final String MINIO_ACCESS_KEY = "MINIO_ROOT_USER";
  private static final String MINIO_SECRET_KEY = "MINIO_ROOT_PASSWORD";

  private static final String DEFAULT_STORAGE_DIRECTORY = "/data";
  private static final String HEALTH_ENDPOINT = "/minio/health/live";

  private String accessKey;
  private String secretKey;

  public MinioContainer(CredentialsProvider credentials) {
    this(DEFAULT_IMAGE + ":" + DEFAULT_TAG, credentials);
  }

  public MinioContainer(String image, CredentialsProvider credentials) {
    super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);

    withNetworkAliases("minio-" + Base58.randomString(6));

    addExposedPort(DEFAULT_PORT);

    if (credentials != null) {
      accessKey = credentials.accessKey();
      secretKey = credentials.secretKey();

      withEnv(MINIO_ACCESS_KEY, accessKey);
      withEnv(MINIO_SECRET_KEY, secretKey);
    }

    withCommand("server", DEFAULT_STORAGE_DIRECTORY);

    setWaitStrategy(
        new HttpWaitStrategy()
            .forPort(DEFAULT_PORT)
            .forPath(HEALTH_ENDPOINT)
            .withStartupTimeout(Duration.ofMinutes(2)));
  }

  public String getHostAddress() {
    return getHost() + ":" + getMappedPort(DEFAULT_PORT);
  }

  public String getAccessKey() {
    return accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public record CredentialsProvider(String accessKey, String secretKey) {}
}
