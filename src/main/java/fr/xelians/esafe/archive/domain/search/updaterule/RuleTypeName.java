/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import fr.xelians.esafe.referential.domain.RuleType;

public record RuleTypeName(RuleType ruleType, String ruleName) {}
