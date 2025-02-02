/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.MatchAllOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Arrays;
import lombok.ToString;

/*
 * @author Emmanuel Deviller
 */
@ToString
public class MatchAll extends MatchAllOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;
  protected final Root<?> rootEntity;

  public MatchAll(SqlParser<?> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
    this.criteriaBuilder = parser.getCriteriaBuilder();
    this.rootEntity = parser.getRootEntity();
  }

  @Override
  public Predicate create() {
    String[] words = value.toString().trim().split("\\P{L}+");
    Expression<String> key = rootEntity.get(field.getFullName()).as(String.class);
    if (words.length > 0) {
      Predicate[] predicates =
          Arrays.stream(words)
              .map(word -> criteriaBuilder.like(key, "%" + word + "%"))
              .toArray(Predicate[]::new);
      return criteriaBuilder.and(predicates);
    }
    return criteriaBuilder.like(key, "%");
  }
}
