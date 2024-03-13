/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.OrOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import java.util.List;

public class Or extends OrOperator<Query> {

  public Or(DslParser<Query> parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext, node);
  }

  @Override
  public Query create(List<Query> queries) {
    return new Query.Builder().bool(b -> b.should(queries)).build();
  }
}
