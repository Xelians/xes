/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.vitamtest;

import fr.xelians.esafe.testcommon.MinioContainer;
import java.util.logging.Level;
import java.util.logging.LogManager;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.lifecycle.Startables;

public class VitamInit implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  // Ressources
  public static final String RESOURCES = "src/test/resources/";
  public static final String REFERENTIEL = RESOURCES + "/referentiel/";
  public static final String RULE = REFERENTIEL + "rule/";
  public static final String AGENCY = REFERENTIEL + "agency/";
  public static final String ONTOLOGY = REFERENTIEL + "ontology/";
  public static final String PROFILE = REFERENTIEL + "profile/";
  public static final String INGESTCONTRACT = REFERENTIEL + "ingestcontract/";
  public static final String ACCESSCONTRACT = REFERENTIEL + "accesscontract/";
  public static final String SEDA_SIP = RESOURCES + "sedav2/sip/";
  public static final String SEDA_FILING = RESOURCES + "sedav2/filing/";
  public static final String SEDA_HOLDING = RESOURCES + "sedav2/holding/";
  public static final String PDF = RESOURCES + "/pdf/";

  // TestContainers
  private static final PostgreSQLContainer<?> postgres;
  private static final ElasticsearchContainer elastic;
  private static final MinioContainer minio;

  private static final String POSTGRES_IMAGE = "postgres:16.0-alpine";
  private static final String ELASTIC_IMAGE =
      "docker.elastic.co/elasticsearch/elasticsearch:8.11.0";

  // Init Tests containers
  static {
    LogManager.getLogManager().getLogger("").setLevel(Level.OFF);

    postgres = createPostgreslContainer();
    elastic = createElasticSearchContainer();
    minio = createMinioContainer();
    Startables.deepStart(elastic, postgres, minio).join();
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
    return new PostgreSQLContainer<>(POSTGRES_IMAGE).withReuse(false);
  }

  @Override
  public void initialize(ConfigurableApplicationContext ctx) {
    // @formatter:off

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
            "app.storage.offer.s3[0].name=minio01",
            "app.storage.offer.s3[0].region=US_EAST_1",
            "app.storage.offer.s3[0].endpoint=http://" + minio.getHostAddress(),
            "app.storage.offer.s3[0].accessKeyId=" + minio.getAccessKey(),
            "app.storage.offer.s3[0].secretAccessKey=" + minio.getSecretKey(),
            "app.storage.offer.s3[0].concurrency=16")
        .applyTo(ctx.getEnvironment());

    // @formatter:on
  }
}
