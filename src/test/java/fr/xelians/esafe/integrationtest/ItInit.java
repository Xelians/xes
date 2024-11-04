/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.integrationtest;

import fr.xelians.esafe.testcommon.ClamAVContainer;
import fr.xelians.esafe.testcommon.MinioContainer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;

public class ItInit implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  // Ressources
  public static final String RESOURCES = "src/test/resources/";

  public static final String REFERENTIAL = RESOURCES + "/referentiel/";
  public static final String RULE = REFERENTIAL + "rule/";
  public static final String AGENCY = REFERENTIAL + "agency/";
  public static final String ONTOLOGY = REFERENTIAL + "ontology/";
  public static final String PROFILE = REFERENTIAL + "profile/";
  public static final String INGEST_CONTRACT = REFERENTIAL + "ingestcontract/";
  public static final String ACCESS_CONTRACT = REFERENTIAL + "accesscontract/";
  public static final String SEDA_SIP = RESOURCES + "sedav2/sip/";
  public static final String SEDA_FILING = RESOURCES + "sedav2/filing/";
  public static final String SEDA_HOLDING = RESOURCES + "sedav2/holding/";
  public static final String PDF = RESOURCES + "pdf/";

  // TestContainers
  private static final ClamAVContainer clamav;
  private static final PostgreSQLContainer<?> postgres;
  private static final ElasticsearchContainer elastic;
  private static final MinioContainer minio;

  private static final String POSTGRES_IMAGE = "postgres:16.1-alpine";
  private static final String ELASTIC_IMAGE =
      "docker.elastic.co/elasticsearch/elasticsearch:8.13.0";

  // Init Tests containers
  static {
    LogManager.getLogManager().getLogger("").setLevel(Level.OFF);

    clamav = createClamAVContainer();
    postgres = createPostgreslContainer();
    elastic = createElasticSearchContainer();
    minio = createMinioContainer();
    Startables.deepStart(elastic, clamav, postgres, minio).join();
  }

  private static ClamAVContainer createClamAVContainer() {
    return new ClamAVContainer().withReuse(false);
  }

  private static MinioContainer createMinioContainer() {
    return new MinioContainer(new MinioContainer.CredentialsProvider("access_key", "secret_key"))
        .withReuse(false);
  }

  private static ElasticsearchContainer createElasticSearchContainer() {
    return new ElasticsearchContainer(ELASTIC_IMAGE)
        .withPassword("elastic")
        .withEnv("xpack.security.enabled", "false")
        .withReuse(true);
  }

  private static PostgreSQLContainer<?> createPostgreslContainer() {
    return new PostgreSQLContainer<>(POSTGRES_IMAGE)
        .withReuse(false)
        .withCommand("postgres", "-c", "log_statement=all");
  }

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    TestPropertyValues.of("antivirus.name=ClamAV", "antivirus.hosts=" + clamav.getHostAddress())
        .applyTo(ctx.getEnvironment());

    String[] tokens = elastic.getHttpHostAddress().split(":");
    TestPropertyValues.of(
            "elasticsearch.host=" + tokens[0],
            "elasticsearch.port=" + tokens[1],
            "elasticsearch.password=elastic")
        .applyTo(ctx.getEnvironment());

    TestPropertyValues.of(
            "spring.datasource.driver-class-name=" + postgres.getDriverClassName(),
            "spring.datasource.url=" + postgres.getJdbcUrl(),
            "spring.datasource.username=" + postgres.getUsername(),
            "spring.datasource.password=" + postgres.getPassword())
        .applyTo(ctx.getEnvironment());

    TestPropertyValues.of(
            "app.storage.offer.s3[0].provider=MINIO",
            "app.storage.offer.s3[0].name=minio01",
            "app.storage.offer.s3[0].region=US_EAST_1",
            "app.storage.offer.s3[0].endpoint=http://" + minio.getHostAddress(),
            "app.storage.offer.s3[0].accessKeyId=" + minio.getAccessKey(),
            "app.storage.offer.s3[0].secretAccessKey=" + minio.getSecretKey(),
            "app.storage.offer.s3[0].concurrency=16")
        .applyTo(ctx.getEnvironment());
  }
}
