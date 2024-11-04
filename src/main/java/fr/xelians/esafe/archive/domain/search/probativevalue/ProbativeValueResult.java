/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import java.util.List;
import java.util.Set;

/*
 * @author Emmanuel Deviller
 */
public record ProbativeValueResult<T>(
    List<T> results, Set<BinaryQualifier> usages, String version) {}
