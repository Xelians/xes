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
import java.util.Iterator;
import java.util.Map.Entry;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@ToString
public abstract class ExistsOperator<T> extends LeafOperator<T> {

  public static final String CREATION_FAILED = "Failed to create query with %s operator";

  protected Field field;
  protected boolean value;

  //  "$exists": "Directeur.Prenom",
  //  "$exists": { "Directeur.Prenom" : true/false, "$type": "DOCTYPE-000001"}
  protected ExistsOperator(DslParser<T> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext);
    setParameter(parameter);
  }

  @Override
  public String name() {
    return "$exists";
  }

  protected void setParameter(JsonNode node) {
    String fieldName = null;

    // Extract JSON Parameters
    if (node.isObject()) {
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
          JsonNode valueNode = entry.getValue();
          if (!valueNode.isBoolean()) {
            throwBadRequestException(
                String.format("$field '%s' value '%s' is not a boolean", fieldName, valueNode));
          }
          value = valueNode.asBoolean();
        } else {
          throwBadRequestException(String.format("Field '%s' is already defined", fieldName));
        }
      }
    } else if (node.isTextual()) {
      fieldName = node.textValue();
      value = true;
    } else {
      throwBadRequestException(String.format("Value '%s' is not valid parameter", node));
    }

    // Compute and check parameters
    if (StringUtils.isBlank(fieldName)) {
      throwBadRequestException("Field is empty or does not exist");
    }

    // Check and get field
    field = parser.getQueryField(docType, fieldName);
  }

  private void throwBadRequestException(String message) {
    throw new BadRequestException(String.format(CREATION_FAILED, this.name()), message);
  }
}
