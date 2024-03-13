/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.AndOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.ToString;

@ToString
public class And extends AndOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;

  public And(SqlParser<?> parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext, node);
    this.criteriaBuilder = parser.getCriteriaBuilder();
  }

  @Override
  public Predicate create(List<Predicate> predicates) {
    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
  }
}
