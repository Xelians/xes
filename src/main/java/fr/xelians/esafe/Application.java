/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/*
 * ElasticsearchClientAutoConfiguration.class is excluded in order to configure Elastic
 * from specific properties (see. ElasticSearchConfig)
 *
 * @author Emmanuel Deviller
 */
@Slf4j
@SpringBootApplication(exclude = ElasticsearchClientAutoConfiguration.class)
public class Application implements CommandLineRunner {

  private final Environment environment;

  @Value("${spring.application.name}")
  private String appName;

  public Application(Environment environment) {
    this.environment = environment;
  }

  public static void main(final String[] args) {
    SpringApplication application = new SpringApplicationBuilder(Application.class).build();
    application.run(args);
  }

  @Override
  public void run(final String... args) {
    log.info(appName + " - SpringBoot Application started");
    log.info("Active profiles: " + Arrays.asList(environment.getActiveProfiles()));
  }

  @Bean
  public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
    return args -> {
      if (log.isDebugEnabled()) {
        System.err.println("Let's inspect the beans provided by Spring Boot:");
        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
          System.err.println(beanName);
        }
      }
    };
  }
}
