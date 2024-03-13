/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.RangeOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

@ToString
public class Range extends RangeOperator<Query> {

  public Range(DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    RangeQuery.Builder builder = new RangeQuery.Builder().field(field.getFullName());
    if (ltValue != null) builder.lt(JsonData.of(ltValue.toString()));
    if (lteValue != null) builder.lte(JsonData.of(lteValue.toString()));
    if (gtValue != null) builder.gt(JsonData.of(gtValue.toString()));
    if (gteValue != null) builder.gte(JsonData.of(gteValue.toString()));

    return doctypeQuery(builder.build()._toQuery());
  }
}
