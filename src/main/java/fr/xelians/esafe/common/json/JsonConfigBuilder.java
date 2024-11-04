/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.json;

/*
 * @author Emmanuel Deviller
 */
public class JsonConfigBuilder {

  private Boolean format;

  private JsonConfigBuilder() {
    format = false;
  }

  public static JsonConfigBuilder builder() {
    return new JsonConfigBuilder();
  }

  public JsonConfigBuilder format(boolean format) {
    this.format = format;
    return this;
  }

  public JsonConfig build() {
    return new JsonConfig(format);
  }
}
