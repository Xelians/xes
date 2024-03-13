/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public interface PredicateSupplier {
  <Y extends Comparable<? super Y>> Predicate getPredicate(Expression<? extends Y> x, Y y);
}
