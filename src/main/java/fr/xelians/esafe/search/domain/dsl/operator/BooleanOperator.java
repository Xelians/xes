/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.parser.DslParser;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import java.util.List;

// A boolean operator class is not a terminal operator and contains other(s) operator(s)
public abstract class BooleanOperator<T> implements BooleanQueryOperator<T> {

  protected final List<Operator> operators;

  protected BooleanOperator(DslParser<T> parser, SearchContext searchContext, JsonNode node) {
    operators = parser.createOperators(searchContext, node);
  }

  @Override
  public List<Operator> getOperators() {
    return operators;
  }
}
