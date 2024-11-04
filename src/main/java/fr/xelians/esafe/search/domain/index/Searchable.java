/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.index;

import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.utils.FieldUtils;
import fr.xelians.esafe.search.domain.dsl.parser.FieldContext;
import fr.xelians.esafe.search.domain.dsl.parser.NamedField;
import fr.xelians.esafe.search.domain.field.Field;
import org.apache.commons.lang.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public interface Searchable {

  String getName();

  String getAlias();

  Field getField(String fieldName);

  boolean isExtField(String fieldName);

  boolean isStdField(String fieldName);

  boolean isSpecialField(String fieldName);

  String getSpecialFieldName(String fieldName, FieldContext type);

  Field getAliasField(String fieldName, FieldContext fieldContext);

  default NamedField getSpecialNamedField(String fieldName, FieldContext fieldContext) {
    String name;
    int i = fieldName.indexOf('.');
    if (i > 0) {
      String lastName = fieldName.substring(i);
      if (FieldUtils.isNotAlphaNumeric(lastName)) {
        throw new BadRequestException(
            "Failed to caret query",
            String.format("Field '%s' must only contain alpha numeric characters", fieldName));
      }
      name = getSpecialFieldName(fieldName.substring(0, i), fieldContext) + lastName;
    } else {
      name = getSpecialFieldName(fieldName, fieldContext);
    }

    if (StringUtils.isBlank(name)) {
      throw new BadRequestException(
          "Failed to caret query",
          String.format("Field '%s' is not a valid special field", fieldName));
    }

    return new NamedField(name, getField(name));
  }
}
