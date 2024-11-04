/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/*
 * @author Emmanuel Deviller
 */
public final class JsonPatchOp {

  public static final String OP = "op";

  public static final String OP_ADD = "add";
  public static final String OP_REPLACE = "replace";
  public static final String OP_REMOVE = "remove";
  public static final String OP_MOVE = "move";
  public static final String OP_COPY = "copy";

  public static final String PATH = "path";
  public static final String VALUE = "value";
  public static final String FROM = "from";

  private JsonPatchOp() {}

  public static JsonNode add(String path, String value) {
    return JsonNodeFactory.instance.objectNode().put(OP, OP_ADD).put(PATH, path).put(VALUE, value);
  }

  public static JsonNode add(String path, JsonNode valueNode) {
    return JsonNodeFactory.instance
        .objectNode()
        .put(OP, OP_ADD)
        .put(PATH, path)
        .set(VALUE, valueNode);
  }

  public static JsonNode replace(String path, String value) {
    return JsonNodeFactory.instance
        .objectNode()
        .put(OP, OP_REPLACE)
        .put(PATH, path)
        .put(VALUE, value);
  }

  public static ObjectNode remove(String path) {
    return JsonNodeFactory.instance.objectNode().put(OP, OP_REMOVE).put(PATH, path);
  }

  public static ObjectNode move(String from, String value) {
    return JsonNodeFactory.instance.objectNode().put(OP, OP_MOVE).put(FROM, from).put(VALUE, value);
  }

  public static ObjectNode copy(String from, String value) {
    return JsonNodeFactory.instance.objectNode().put(OP, OP_COPY).put(FROM, from).put(VALUE, value);
  }
}
