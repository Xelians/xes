/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.xelians.esafe.search.domain.SearchEngineProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
@Slf4j
public class ElasticSearchConfig {

  private final SearchEngineProperties searchEngineProperties;

  @Bean
  RestClient restClient() {
    String host = searchEngineProperties.getHost();
    int port = searchEngineProperties.getPort();
    String username = searchEngineProperties.getUsername();
    String password = searchEngineProperties.getPassword();
    log.info("host: {} - port: {} -  username {} - password {}", host, port, username, password);

    // Create the low-level client
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(
        AuthScope.ANY, new UsernamePasswordCredentials(username, password));

    return RestClient.builder(new HttpHost(host, port, "http"))
        .setHttpClientConfigCallback(b -> b.setDefaultCredentialsProvider(credentialsProvider))
        .build();
  }

  @Bean
  ElasticsearchClient elasticsearchClient(RestClient restClient) {
    JacksonJsonpMapper jjMapper = new JacksonJsonpMapper();
    ObjectMapper objectMapper = jjMapper.objectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    ElasticsearchTransport transport = new RestClientTransport(restClient, jjMapper);
    return new ElasticsearchClient(transport);
  }
}
