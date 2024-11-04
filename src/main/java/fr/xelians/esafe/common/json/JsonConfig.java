/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.json;

/*
 * @author Emmanuel Deviller
 */
public record JsonConfig(boolean format) {

  public static final JsonConfig DEFAULT = JsonConfigBuilder.builder().build();

  @Override
  public String toString() {
    return "JsonConfig{" + "format=" + format + "'}'";
  }
}
