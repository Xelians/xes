/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import fr.xelians.esafe.archive.domain.unit.rules.management.AbstractRules;
import fr.xelians.esafe.referential.domain.RuleType;
import java.util.List;
import java.util.Map;

public record RuleLists(
    List<RuleTypeName> deleteRules,
    Map<RuleType, UpdateRules> updateRules,
    List<AbstractRules> addRules) {}
