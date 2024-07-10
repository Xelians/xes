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
public abstract class NotOperator<T> extends BooleanOperator<T> {

  protected NotOperator(DslParser<T> parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext, node);
  }

  @Override
  public String name() {
    return "$not";
  }
}
