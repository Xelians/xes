/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.operator.sql;

import fr.xelians.esafe.referential.domain.Status;
import fr.xelians.esafe.search.domain.field.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;

/*
 * @author Emmanuel Deviller
 */
public class Predicator {

  private Predicator() {}

  public static Predicate predicate(
      Field field, Object value, Root<?> rootEntity, PredicateSupplier supplier) {
    return switch (field.getType()) {
      case IntegerField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (Integer) value);
      case LongField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (Long) value);
      case DoubleField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (Double) value);
      case BooleanField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (Boolean) value);
      case DateField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (LocalDateTime) value);
      case StatusField.TYPE -> supplier.getPredicate(
          rootEntity.get(field.getFullName()), (Status) value);
      default -> supplier.getPredicate(rootEntity.get(field.getFullName()), value.toString());
    };
  }
}
