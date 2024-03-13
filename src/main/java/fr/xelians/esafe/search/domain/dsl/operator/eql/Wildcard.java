/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.WildcardOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

@ToString
public class Wildcard extends WildcardOperator<Query> {

  // { "$wildcard": { "societe.facture": "clie*nt1" } , "$type": "Facture"  }
  public Wildcard(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    Query query =
        WildcardQuery.of(t -> t.field(field.getFullName()).value(value.toString()))._toQuery();
    return doctypeQuery(query);
  }
}
