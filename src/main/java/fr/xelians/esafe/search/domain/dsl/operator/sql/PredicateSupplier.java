/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/*
 * @author Emmanuel Deviller
 */
public interface PredicateSupplier {
  <Y extends Comparable<? super Y>> Predicate getPredicate(Expression<? extends Y> x, Y y);
}
