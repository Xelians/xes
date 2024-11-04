/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.antivirus;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/*
 * @author Emmanuel Deviller
 */
@Data
@Component
@ConfigurationProperties(prefix = "antivirus")
@Validated
public class AntiVirusProperties {

  private AntiVirus name = AntiVirus.None;

  private String[] hosts = {"localhost"};

  @Min(0)
  private int timeout = 10000;

  @Min(0)
  private long scanLength = 16384;
}
