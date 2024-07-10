/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class JsonPatchBuilder {

  private final ArrayNode jsonPatchs;

  public JsonPatchBuilder() {
    jsonPatchs = JsonNodeFactory.instance.arrayNode();
  }

  public JsonPatchBuilder op(JsonNode node) {
    if (node.isArray()) node.forEach(jsonPatchs::add);
    else jsonPatchs.add(node);
    return this;
  }

  public JsonPatchBuilder add(String path, String value) {
    jsonPatchs.add(JsonPatchOp.add(path, value));
    return this;
  }

  public JsonPatchBuilder add(String path, JsonNode valueNode) {
    jsonPatchs.add(JsonPatchOp.add(path, valueNode));
    return this;
  }

  public JsonPatchBuilder replace(String path, String value) {
    jsonPatchs.add(JsonPatchOp.replace(path, value));
    return this;
  }

  public JsonPatchBuilder remove(String path) {
    jsonPatchs.add(JsonPatchOp.remove(path));
    return this;
  }

  public JsonPatchBuilder move(String from, String value) {
    jsonPatchs.add(JsonPatchOp.move(from, value));
    return this;
  }

  public JsonPatchBuilder copy(String from, String value) {
    jsonPatchs.add(JsonPatchOp.copy(from, value));
    return this;
  }

  public JsonNode build() {
    return jsonPatchs;
  }
}
