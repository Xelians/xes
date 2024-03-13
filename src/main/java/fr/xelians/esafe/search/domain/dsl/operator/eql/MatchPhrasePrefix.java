/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.eql;

import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.MatchPhrasePrefixOperator;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

@ToString
public class MatchPhrasePrefix extends MatchPhrasePrefixOperator<Query> {

  // { "$match_phrase_prefix": { "Title": "Porte de Bagnolet", "$type": "Contrat" } },
  public MatchPhrasePrefix(
      DslParser<Query> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
  }

  @Override
  public Query create() {
    Query query =
        MatchPhrasePrefixQuery.of(t -> t.field(field.getFullName()).query(value.toString()))
            ._toQuery();
    return doctypeQuery(query);
  }
}
