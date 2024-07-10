/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import fr.xelians.esafe.storage.domain.StorageType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Duration;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;

@Data
@Validated
public class S3Storage {

  /*
   Restrictions: https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html
   Restriction for 50 characters are applied due to the dynamic suffix during the creation (_tenant_XXX).
   Full controls have not been implemented, just a general guideline.
  */
  @NotBlank
  @Pattern(regexp = "^[a-z0-9]([a-z0-9-.]{0,50}[a-z0-9])?$")
  private String name;

  private Region region = Region.EU_WEST_1;

  private URI endpoint = null;

  @NotBlank private String accessKeyId;

  @NotBlank private String secretAccessKey;

  @Min(1)
  @Max(1024)
  private int concurrency = 32;

  private boolean isActive = true;

  public StorageType getStorageTye() {
    return StorageType.S3;
  }

  public S3Client createS3Client() {
    S3Configuration configuration =
        S3Configuration.builder()
            .checksumValidationEnabled(false)
            .pathStyleAccessEnabled(true)
            .build();

    AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

    // Default CRT Client
    S3ClientBuilder s3ClientBuilder =
        S3Client.builder()
            .httpClientBuilder(
                AwsCrtHttpClient.builder()
                    .maxConcurrency(concurrency)
                    .connectionTimeout(Duration.ofSeconds(10)))
            .region(region)
            .serviceConfiguration(configuration)
            .credentialsProvider(credentialsProvider);

    if (endpoint != null) {
      s3ClientBuilder.endpointOverride(endpoint);
    }
    return s3ClientBuilder.build();
  }

  public S3AsyncClient createS3AsyncClient2() {

    AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);
    S3CrtHttpConfiguration configuration =
        S3CrtHttpConfiguration.builder().connectionTimeout(Duration.ofSeconds(10)).build();

    S3CrtAsyncClientBuilder builder =
        S3AsyncClient.crtBuilder()
            .httpConfiguration(configuration)
            .region(region)
            .credentialsProvider(credentialsProvider)
            .checksumValidationEnabled(false)
            .forcePathStyle(true)
            // https://github.com/awslabs/aws-crt-java/issues/686
            // .httpConfiguration(e -> e.trustAllCertificatesEnabled(true))
            .maxConcurrency(concurrency)
            .targetThroughputInGbps(10d)
            .minimumPartSizeInBytes(8_000_000L);

    if (endpoint != null) {
      builder.endpointOverride(endpoint);
    }
    return builder.build();
  }

  public S3AsyncClient createS3AsyncClient() {
    SdkAsyncHttpClient httpClient =
        NettyNioAsyncHttpClient.builder()
            .maxPendingConnectionAcquires(60000)
            .connectionAcquisitionTimeout(Duration.ofSeconds(10))
            .writeTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(10))
            .maxConcurrency(concurrency)
            .build();

    S3Configuration s3Configuration =
        S3Configuration.builder()
            .checksumValidationEnabled(false)
            .pathStyleAccessEnabled(true)
            .build();

    AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
    StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

    S3AsyncClientBuilder builder =
        S3AsyncClient.builder()
            .region(region)
            .httpClient(httpClient)
            .serviceConfiguration(s3Configuration)
            .credentialsProvider(credentialsProvider);

    if (endpoint != null) {
      builder.endpointOverride(endpoint);
    }
    return builder.build();
  }
}

//  public synchronized S3Client getS3Client() {
//    if (s3Client == null) {
//      S3Configuration configuration =
//          S3Configuration.builder()
//              .checksumValidationEnabled(false)
//              .pathStyleAccessEnabled(true)
//              .build();
//
//      AwsCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
//      StaticCredentialsProvider credentialsProvider =
// StaticCredentialsProvider.create(credentials);
//
//      // Default Apache Client
//      S3ClientBuilder s3ClientBuilder =
//          S3Client.builder()
//              .httpClientBuilder(
//                  ApacheHttpClient.builder()
//                      .maxConnections(concurrency)
//                      .connectionTimeout(Duration.ofSeconds(10)))
//              .region(region)
//              .serviceConfiguration(configuration)
//              .credentialsProvider(credentialsProvider);
//
//      if (endpoint != null) {
//        s3ClientBuilder.endpointOverride(endpoint);
//      }
//
//      s3Client = s3ClientBuilder.build();
//    }
//
//    return s3Client;
//  }
