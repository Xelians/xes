/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.search.domain.field.DoubleField;
import fr.xelians.esafe.search.domain.field.Field;
import fr.xelians.esafe.search.domain.field.UndefinedField;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public final class FieldUtils {

  private static final String TYPE = "type";
  private static final String PROPERTIES = "properties";
  private static final String FIELDS = "fields";

  private FieldUtils() {}

  public static Map<String, Field> buildStandardFields(String mapping) {
    Map<String, Field> fields = new HashMap<>();
    try {
      ObjectMapper mapper = new ObjectMapper();

      // Add Elastic Search predefined score field
      // Note. The score field works in the orderby but has no effect elsewhere
      Field field = Field.create("_score", DoubleField.TYPE, true);
      fields.put(field.getName(), field);

      JsonNode node = mapper.readTree(mapping);
      visitFields(node, "", fields);
    } catch (JsonProcessingException ex) {
      throw new InternalException(
          "Failed to create archive unit standard fields ",
          String.format("Failed to parse mapping '%s'", mapping),
          ex);
    }
    return fields;
  }

  private static void visitFields(JsonNode node, String path, Map<String, Field> fields) {
    if (node.isObject()) {
      for (Iterator<Entry<String, JsonNode>> i = node.fields(); i.hasNext(); ) {
        Entry<String, JsonNode> entry = i.next();
        String key = entry.getKey();
        JsonNode value = entry.getValue();
        if (TYPE.equals(key) && !value.isObject()) {
          // A "type" field can exist in mapping
          if (!path.startsWith(Field.EXT)) {
            Field field = Field.create(path, value.asText(), true);
            fields.put(field.getName(), field);
          }
        } else if (PROPERTIES.equals(key)) {
          // "properties" or "fields" must not exist in mapping...
          if (!path.startsWith(Field.EXT)) {
            Field field = new UndefinedField(path);
            fields.put(field.getName(), field);
          }
          visitFields(value, path, fields);
        } else if (FIELDS.equals(key)) {
          visitFields(value, path, fields);
        } else {
          visitFields(value, path.isEmpty() ? key : path + "." + key, fields);
        }
      }
    }
  }

  public static boolean isNotAlphaNumeric(String fieldName) {
    String[] tokens = StringUtils.split(fieldName, '.');
    for (String token : tokens) {
      if (!StringUtils.isAlphanumeric(token)) {
        return true;
      }
    }
    return false;
  }
}
