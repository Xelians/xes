/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import io.micrometer.observation.ObservationRegistry;
import javax.sql.DataSource;
import net.ttddyy.observation.boot.autoconfigure.DataSourceNameResolver;
import net.ttddyy.observation.boot.autoconfigure.DataSourceObservationAutoConfiguration;
import net.ttddyy.observation.boot.autoconfigure.DefaultDataSourceNameResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * @author Julien Cornille
 */
@Configuration
@AutoConfiguration(before = DataSourceObservationAutoConfiguration.class)
@ConditionalOnClass({DataSource.class, ObservationRegistry.class})
public class ProjectDataSourceObservationAutoConfiguration {
  @Bean
  public DataSourceNameResolver defaultDataSourceNameResolver() {
    return new DefaultDataSourceNameResolver();
  }
}
