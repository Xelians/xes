/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.domain.search;

import fr.xelians.esafe.common.utils.CollUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import java.util.Collections;
import java.util.Map;

public final class ReferentialSpecialFields {

  private static final Map<String, String> BASE_FIELDS = createBaseFields();
  private static final Map<String, String> QUERY_FIELDS = createQueryFields();
  private static final Map<String, String> PROJECTION_FIELDS = createProjectionFields();
  private static final Map<String, String> UPDATE_FIELDS = createUpdateFields();
  private static final Map<String, String> RECLASSIFICATION_FIELDS = createReclassificationFields();

  private ReferentialSpecialFields() {}

  // Don't add score. It does not exist in postgresql (maybe we could map #score to _id instead)
  private static Map<String, String> createBaseFields() {
    return Map.ofEntries(Map.entry("#id", "_id"));
  }

  // Unsupported fields : #nbunits, #nbobjects
  private static Map<String, String> createQueryFields() {
    return BASE_FIELDS;
  }

  // Query supported fields
  // Vitam unsupported fields : #nbunits, #nbobjects, #score, #storage
  private static Map<String, String> createProjectionFields() {
    return CollUtils.concatMap(BASE_FIELDS, Map.of("#tenant", "_tenant"));
  }

  // Update supported fields
  private static Map<String, String> createUpdateFields() {
    return Collections.emptyMap();
  }

  // Reclassification supported fields
  // An archive unit accepts one and only one parent.
  // So #unitup and #unitups refer to the same property.
  private static Map<String, String> createReclassificationFields() {
    return Collections.emptyMap();
  }

  public static boolean isSpecial(String fieldName) {
    return fieldName.startsWith("#");
  }

  public static String getFieldName(String name, FieldContext context) {
    return (switch (context) {
          case QUERY -> QUERY_FIELDS;
          case PROJECTION -> PROJECTION_FIELDS;
          case UPDATE -> UPDATE_FIELDS;
          case RECLASSIFICATION -> RECLASSIFICATION_FIELDS;
        })
        .get(name);
  }
}
