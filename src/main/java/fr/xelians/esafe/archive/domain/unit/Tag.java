/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.common.utils.Utils;
import org.apache.commons.lang3.Validate;

/*
 * @author Emmanuel Deviller
 */
public record Tag(@JsonProperty("Key") String key, @JsonProperty("Value") String value) {

  @JsonCreator
  public Tag {
    Validate.notNull(value, Utils.NOT_NULL, "value");
  }
}
