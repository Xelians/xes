/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
