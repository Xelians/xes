/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.ingest.sedav2.ontology;

import fr.xelians.esafe.archive.domain.ingest.Mapping;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.search.domain.field.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
public class OntologyUtils {

  private OntologyUtils() {}

  public static List<Mapping> createMappings(List<String> keys) {
    int textCounter = 0;
    int keywordCounter = 0;
    int dateCounter = 0;
    int longCounter = 0;
    int doubleCounter = 0;

    List<Mapping> mappings = new ArrayList<>();
    for (String value : keys) {
      String[] tokens = StringUtils.split(value, '=');
      String fieldName =
          switch (tokens[1]) {
            case "text" -> TextField.getFieldName(++textCounter);
            case "keyword" -> KeywordField.getFieldName(++keywordCounter);
            case "date" -> DateField.getFieldName(++dateCounter);
            case "long" -> LongField.getFieldName(++longCounter);
            case "double" -> DoubleField.getFieldName(++doubleCounter);
            default -> throw new InternalException(
                "Failed to create Seda v2 mapping",
                String.format("Unknown type '%s' for field '%s'", tokens[1], tokens[0]));
          };
      mappings.add(new Mapping(tokens[0], fieldName));
    }
    return Collections.unmodifiableList(mappings);
  }
}
