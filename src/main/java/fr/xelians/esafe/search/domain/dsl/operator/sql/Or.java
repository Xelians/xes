/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
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

/*
 * @author Emmanuel Deviller
 */
@ToString
public class Or extends AndOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;

  public Or(SqlParser<?> parser, SearchContext searchContext, JsonNode node) {
    super(parser, searchContext, node);
    this.criteriaBuilder = parser.getCriteriaBuilder();
  }

  @Override
  public Predicate create(List<Predicate> predicates) {
    return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
  }
}
