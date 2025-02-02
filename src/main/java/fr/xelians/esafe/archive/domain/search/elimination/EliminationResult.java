/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.elimination;

import java.time.LocalDate;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record EliminationResult<T>(List<T> results, LocalDate eliminationDate) {}
