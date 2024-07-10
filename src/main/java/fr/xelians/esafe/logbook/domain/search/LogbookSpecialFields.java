/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.logbook.domain.search;

import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.CollUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import java.util.Map;

public final class LogbookSpecialFields {

  private static final Map<String, String> BASE_FIELDS = createBaseFields();
  private static final Map<String, String> QUERY_FIELDS = createQueryFields();
  private static final Map<String, String> PROJECTION_FIELDS = createProjectionFields();

  private LogbookSpecialFields() {}

  private static Map<String, String> createBaseFields() {
    return Map.ofEntries(
        Map.entry("#id", "_operationId"),
        Map.entry("#creation", "_creationDate"),
        Map.entry("#update", "_updateDate"),
        Map.entry("#score", "_score"));
  }

  private static Map<String, String> createQueryFields() {
    return BASE_FIELDS;
  }

  private static Map<String, String> createProjectionFields() {
    return CollUtils.concatMap(BASE_FIELDS, Map.of("#tenant", "_tenant"));
  }

  public static boolean isSpecial(String fieldName) {
    return fieldName.startsWith("#");
  }

  public static String getFieldName(String name, FieldContext context) {
    return (switch (context) {
          case QUERY -> QUERY_FIELDS;
          case PROJECTION -> PROJECTION_FIELDS;
          default -> throw new InternalException(
              String.format("Bad context '%s' for logbook special field", context));
        })
        .get(name);
  }
}
