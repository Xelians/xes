/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.search.domain.dsl.operator.MatchPhrasePrefixOperator;
import fr.xelians.esafe.search.domain.dsl.parser.SearchContext;
import fr.xelians.esafe.search.domain.dsl.parser.sql.SqlParser;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.ToString;

@ToString
public class MatchPhrasePrefix extends MatchPhrasePrefixOperator<Predicate> {

  protected final CriteriaBuilder criteriaBuilder;
  protected final Root<?> rootEntity;

  public MatchPhrasePrefix(SqlParser<?> parser, SearchContext searchContext, JsonNode parameter) {
    super(parser, searchContext, parameter);
    this.criteriaBuilder = parser.getCriteriaBuilder();
    this.rootEntity = parser.getRootEntity();
  }

  @Override
  public Predicate create() {
    Expression<String> key = rootEntity.get(field.getFullName()).as(String.class);
    return criteriaBuilder.like(key, value.toString() + "%");
  }
}
