/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration.init;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * @author Julien Cornille
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "app.indexing.on-start")
public class IndexingProperties {

  private boolean reset = false;
  private boolean createIfMissing = true;
}
