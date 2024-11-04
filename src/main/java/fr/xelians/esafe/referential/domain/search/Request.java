/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.referential.domain.search;

import jakarta.persistence.TypedQuery;

/*
 * @author Emmanuel Deviller
 */
public record Request<T>(TypedQuery<T> mainQuery, TypedQuery<Long> countQuery) {}
