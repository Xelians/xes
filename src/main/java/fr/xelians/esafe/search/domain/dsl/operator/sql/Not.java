/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.search.domain.dsl.operator.NotOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import lombok.ToString;

@ToString
public class Not extends NotOperator<Predicate> {

  public static final String CREATION_FAILED = "Failed to create query with %s operator";

  protected final CriteriaBuilder criteriaBuilder;

  public Not(SqlParser<?> parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext, node);
    this.criteriaBuilder = parser.getCriteriaBuilder();
  }

  @Override
  public Predicate create(List<Predicate> predicates) {
    if (predicates.size() > 1) {
      throw new BadRequestException(
          String.format(CREATION_FAILED, this.name()),
          "The not operator does not support more than one argument");
    }
    return criteriaBuilder.not(predicates.getFirst());
  }
}
