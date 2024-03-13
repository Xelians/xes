/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.LtOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.*;
import lombok.ToString;

@ToString
public class Lt extends LtOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;
  protected final Root<?> rootEntity;

  public Lt(SqlParser<?> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
    this.criteriaBuilder = parser.getCriteriaBuilder();
    this.rootEntity = parser.getRootEntity();
  }

  @Override
  public Predicate create() {
    PredicateSupplier supplier = criteriaBuilder::lessThan;
    return Predicator.predicate(field, value, rootEntity, supplier);
  }
}
