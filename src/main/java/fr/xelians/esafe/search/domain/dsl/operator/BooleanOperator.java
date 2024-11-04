/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import java.util.List;

// A boolean operator class is not a terminal operator and contains other(s) operator(s)
/*
 * @author Emmanuel Deviller
 */
public abstract class BooleanOperator<T> implements BooleanQueryOperator<T> {

  protected final List<Operator> operators;

  protected BooleanOperator(DslParser<T> parser, SearchContext searchContext, JsonNode node) {
    operators = parser.createOperators(searchContext, node);
  }

  @Override
  public List<Operator> getOperators() {
    return operators;
  }
}
