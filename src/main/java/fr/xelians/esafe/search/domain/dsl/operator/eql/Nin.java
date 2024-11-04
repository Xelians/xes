/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.NinOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.field.BooleanField;
import fr.xelians.esafe.search.domain.field.DoubleField;
import fr.xelians.esafe.search.domain.field.IntegerField;
import fr.xelians.esafe.search.domain.field.LongField;
import java.util.List;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@ToString
public class Nin extends NinOperator<Query> {

  // { "$nin": { "Title": ["Porte", "de", "Bagnolet" ], "$type": "Contrat"  },
  public Nin(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    List<FieldValue> fieldValues =
        switch (field.getType()) {
          case LongField.TYPE, IntegerField.TYPE, DoubleField.TYPE, BooleanField.TYPE -> values
              .stream()
              .map(FieldValue::of)
              .toList();
          default -> values.stream().map(v -> FieldValue.of(v.toString())).toList();
        };

    TermsQueryField tqf = TermsQueryField.of(t -> t.value(fieldValues));
    Query tquery = TermsQuery.of(t -> t.field(field.getFullName()).terms(tqf))._toQuery();
    Query query = BoolQuery.of(b -> b.mustNot(tquery))._toQuery();
    return doctypeQuery(query);
  }
}
