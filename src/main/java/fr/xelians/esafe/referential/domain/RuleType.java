/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.domain;

import java.util.Arrays;
import java.util.Optional;

/*
 * @author Emmanuel Deviller
 */
public enum RuleType {
  AppraisalRule,
  AccessRule,
  StorageRule,
  DisseminationRule,
  ClassificationRule,
  ReuseRule,
  HoldRule;

  public static Optional<RuleType> fromName(String name) {
    return Arrays.stream(values()).filter(r -> r.name().equalsIgnoreCase(name)).findFirst();
  }
}
