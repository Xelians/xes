/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.MatchOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.field.BooleanField;
import fr.xelians.esafe.search.domain.field.DoubleField;
import fr.xelians.esafe.search.domain.field.IntegerField;
import fr.xelians.esafe.search.domain.field.LongField;
import lombok.ToString;

@ToString
public class Match extends MatchOperator<Query> {

  // { "$match": { "Title": "Porte de Bagnolet", "$type": "Contrat" } },
  public Match(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    FieldValue fieldValue =
        switch (field.getType()) {
          case LongField.TYPE, IntegerField.TYPE, DoubleField.TYPE, BooleanField.TYPE -> FieldValue
              .of(value);
          default -> FieldValue.of(value.toString());
        };

    Query query = MatchQuery.of(t -> t.field(field.getFullName()).query(fieldValue))._toQuery();
    return doctypeQuery(query);
  }
}
