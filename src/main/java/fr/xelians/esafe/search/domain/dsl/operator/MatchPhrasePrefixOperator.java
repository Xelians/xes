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

/*
 * @author Emmanuel Deviller
 */
@ToString
public abstract class MatchPhrasePrefixOperator<T> extends ValueOperator<T> {

  // { "$match_phrase_prefix": { "Title": "Porte de Bagnolet", "$type": "Contrat" } },
  protected MatchPhrasePrefixOperator(
      DslParser<T> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public String name() {
    return "$match_phrase_prefix";
  }
}
