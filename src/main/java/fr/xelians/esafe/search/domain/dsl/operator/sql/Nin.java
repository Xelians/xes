/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.NinOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.ToString;

@ToString
public class Nin extends NinOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;
  protected final Root<?> rootEntity;

  public Nin(SqlParser<?> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
    this.criteriaBuilder = parser.getCriteriaBuilder();
    this.rootEntity = parser.getRootEntity();
  }

  @Override
  public Predicate create() {
    Path<Object> key = rootEntity.get(field.getFullName());
    CriteriaBuilder.In<Object> inPredicate = criteriaBuilder.in(key);
    values.forEach(inPredicate::value);
    return criteriaBuilder.not(inPredicate);
  }
}
