/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

@ToString
public abstract class GtOperator<T> extends ValueOperator<T> {

  // { "$gt": { "client.age": 33 } , "$type": "Company"  }
  protected GtOperator(DslParser<T> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public String name() {
    return "$gt";
  }
}
