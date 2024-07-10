/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.field.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import lombok.ToString;
import org.apache.commons.lang.StringUtils;

@ToString
public abstract class ValuesOperator<T> extends LeafOperator<T> {

  public static final String CREATION_FAILED = "Failed to create query with %s operator";

  protected Field field;
  protected final List<Object> values = new ArrayList<>();

  protected ValuesOperator(DslParser<T> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext);
    setParameter(parameter);
  }

  protected void setParameter(JsonNode node) {
    String fieldName = null;
    JsonNode valueNodes = null;

    // Extract JSON Parameters
    for (Iterator<Entry<String, JsonNode>> i = node.fields(); i.hasNext(); ) {
      Entry<String, JsonNode> entry = i.next();
      String key = entry.getKey();
      if (TYPE.equals(key)) {
        if (parser.getOntologyMapper() == null) {
          throwBadRequestException("The query does not support $docType field");
        }
        docType = entry.getValue().asText();
      } else if (fieldName == null) {
        fieldName = key;
        valueNodes = entry.getValue();
      } else {
        throwBadRequestException(String.format("Field '%s' is already defined", fieldName));
      }
    }

    // Compute and check parameters
    if (StringUtils.isBlank(fieldName)) {
      throwBadRequestException("Field is empty or does not exist");
    }

    if (valueNodes == null || !valueNodes.isArray()) {
      throwBadRequestException(String.format("Field '%s' values must be an array", fieldName));
    }

    // Check and get field
    field = parser.getQueryField(docType, fieldName);

    for (JsonNode valueNode : valueNodes) {
      try {
        values.add(field.asValue(valueNode));
      } catch (BadRequestException ex) {
        throwBadRequestException(
            String.format(
                "Field '%s' with type '%s' and value '%s' of type '%s' mismatch",
                fieldName,
                field.getType(),
                valueNode.asText(),
                valueNode.getClass().getSimpleName().toLowerCase().replace("node", "")));
      }
    }
  }

  private void throwBadRequestException(String message) {
    throw new BadRequestException(String.format(CREATION_FAILED, this.name()), message);
  }
}
