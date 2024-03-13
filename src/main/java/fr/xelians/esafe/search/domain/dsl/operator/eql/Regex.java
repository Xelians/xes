/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RegexpQuery;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.RegexOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

@ToString
public class Regex extends RegexOperator<Query> {

  // { "$eq": { "societe.facture": "client1" } , "$type": "Facture"  }
  public Regex(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    Query query =
        RegexpQuery.of(t -> t.field(field.getFullName()).value(value.toString()))._toQuery();
    return doctypeQuery(query);
  }
}
