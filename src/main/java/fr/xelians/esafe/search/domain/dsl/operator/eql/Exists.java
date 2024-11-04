/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.ExistsOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@ToString
public class Exists extends ExistsOperator<Query> {

  //  "$exists": "Directeur.Prenom",
  //  "$exists": { "Directeur.Prenom" : true/false, "$type": "DOCTYPE-000001"}
  public Exists(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    Query equery = ExistsQuery.of(t -> t.field(field.getFullName()))._toQuery();
    Query query = value ? equery : BoolQuery.of(b -> b.mustNot(equery))._toQuery();
    return doctypeQuery(query);
  }
}
