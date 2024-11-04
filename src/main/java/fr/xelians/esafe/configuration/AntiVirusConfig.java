/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration;

import fr.xelians.esafe.antivirus.AntiVirus;
import fr.xelians.esafe.antivirus.AntiVirusProperties;
import fr.xelians.esafe.antivirus.AntiVirusScanner;
import fr.xelians.esafe.antivirus.NoneAvScanner;
import fr.xelians.esafe.antivirus.clamav.ClamAvScanner;
import fr.xelians.esafe.common.exception.technical.InternalException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * @author Emmanuel Deviller
 */
@Configuration
@AllArgsConstructor
@Slf4j
public class AntiVirusConfig {

  private final AntiVirusProperties properties;

  @Bean
  AntiVirusScanner antiVirusScanner() {
    return switch (properties.getName()) {
      case AntiVirus.Rest -> throw new InternalException(
          "Rest http anti virus is not yet implemented");
      case AntiVirus.None -> NoneAvScanner.INSTANCE;
      case AntiVirus.ClamAV -> new ClamAvScanner(
          properties.getHosts(), properties.getTimeout(), properties.getScanLength());
    };
  }
}
