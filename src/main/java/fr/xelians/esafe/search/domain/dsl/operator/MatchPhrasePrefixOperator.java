/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import lombok.ToString;

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
