/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import fr.xelians.esafe.common.filter.LoggingFilter;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/*
 * @author Emmanuel Deviller
 */
@Configuration
@EnableScheduling
@EnableJpaRepositories("fr.xelians.esafe")
@EntityScan("fr.xelians.esafe")
@Import(ProjectDataSourceObservationAutoConfiguration.class)
public class ApplicationConfig {

  // Trim properties values
  @Bean
  public static PropertySourcesPlaceholderConfigurer createPropertyConfigurer() {
    PropertySourcesPlaceholderConfigurer propertyConfigurer =
        new PropertySourcesPlaceholderConfigurer();
    propertyConfigurer.setTrimValues(true);
    return propertyConfigurer;
  }

  @Bean
  @Profile("!openapi-gen")
  public TaskScheduler taskScheduler() {
    // Allow several scheduled threads (default is 1)
    return new ConcurrentTaskScheduler(new ScheduledThreadPoolExecutor(10));
  }

  /**
   * The filter adds an ETag header to all GET responses containing a hash value of the resource’s
   * content
   */
  // @Bean
  public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
    return new ShallowEtagHeaderFilter();
  }

  @Bean
  public LoggingFilter logFilter(
      final @Value("${app.logging.request.path-to-ignore}") String[] pathsToIgnore,
      final @Value("${app.logging.request.includeHeaders:false}") boolean includeHeaders,
      final @Value("${app.logging.request.includePayload:true}") boolean includePayload) {
    LoggingFilter filter = new LoggingFilter(pathsToIgnore);
    filter.setIncludeQueryString(true);
    filter.setIncludePayload(includePayload);
    filter.setMaxPayloadLength(Integer.MAX_VALUE);
    filter.setIncludeHeaders(includeHeaders);
    return filter;
  }
}
