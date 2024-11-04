/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import co.elastic.clients.json.JsonpSerializable;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.archive.domain.unit.KeyTag;
import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public final class JsonUtils {

  private JsonUtils() {}

  public static void writeStringsField(
      JsonGenerator generator, String fieldName, Collection<String> values) throws IOException {
    generator.writeFieldName(fieldName);
    generator.writeStartArray();
    for (String str : values) {
      generator.writeString(str);
    }
    generator.writeEndArray();
  }

  public static List<Long> toLongs(JsonNode jsonNode) {
    if (jsonNode.isArray()) {
      List<Long> list = new ArrayList<>();
      for (JsonNode node : jsonNode) {
        list.add(node.asLong());
      }
      return list;
    }
    throw new InternalException("JsonNode is not an array");
  }

  public static List<String> toStrings(JsonNode jsonNode) {
    if (jsonNode.isArray()) {
      List<String> list = new ArrayList<>();
      for (JsonNode node : jsonNode) {
        list.add(node.asText());
      }
      return list;
    }
    throw new InternalException("JsonNode is not an array");
  }

  public static String asText(JsonNode node) {
    return node == null ? null : node.asText();
  }

  public static String toJson(JsonpSerializable request) {
    StringWriter writer = new StringWriter();
    try (JacksonJsonpGenerator generator =
        new JacksonJsonpGenerator(new JsonFactory().createGenerator(writer))) {
      request.serialize(generator, new JacksonJsonpMapper());
    } catch (IOException e) {
      // This should never happen because we use a string writer
      throw new InternalException(e);
    }
    return writer.toString();
  }

  public static List<KeyTag> visit(JsonNode node) {
    return visit("", node, new ArrayList<>());
  }

  private static List<KeyTag> visit(String path, JsonNode node, List<KeyTag> keyTags) {
    if (node.isObject()) {
      String p = path.isEmpty() ? "" : path + ".";
      node.fields().forEachRemaining(e -> visit(p + e.getKey(), e.getValue(), keyTags));
    } else if (node.isArray()) {
      node.forEach(n -> visit(path, n, keyTags));
    } else {
      keyTags.add(new KeyTag(path, node.asText()));
    }
    return keyTags;
  }

  public static long calculateSize(JsonNode jsonNode) {
    if (jsonNode == null) {
      return 4L; // null
    }
    if (jsonNode.isTextual()) {
      return 2L + jsonNode.asText().length(); // two quotes + string
    }
    if (jsonNode.isObject()) {
      long size = 2L; // curly brackets
      for (var ite = jsonNode.fields(); ite.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = ite.next();
        // quotes, colon, commas,  key, value
        size += 4 + entry.getKey().length() + calculateSize(entry.getValue());
      }
      return jsonNode.isEmpty() ? size : size - 1L;
    }
    if (jsonNode.isArray()) {
      long size = 2; // square brackets
      for (JsonNode node : jsonNode) {
        size += calculateSize(node) + 1L;
      }
      return jsonNode.isEmpty() ? size : size - 1;
    }
    return jsonNode.asText().length(); // boolean, number
  }
}
