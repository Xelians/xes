/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/*
 * @author Emmanuel Deviller
 */
@Data
@Component
@ConfigurationProperties(prefix = "elasticsearch")
@Validated
public class SearchEngineProperties {

  @NotBlank private String host;

  @Min(1)
  private int port;

  @NotBlank private String username;

  @NotBlank private String password;
}
