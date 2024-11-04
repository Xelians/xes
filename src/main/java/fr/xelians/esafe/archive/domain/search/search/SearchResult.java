/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.search.domain.dsl.bucket.Facet;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record SearchResult<T>(
    @JsonProperty("httpCode") int httpCode,
    @JsonProperty("$hits") Hits hits,
    @JsonProperty("$results") List<T> results,
    @JsonProperty("$facetResults") List<Facet> facets,
    @JsonProperty("$context") SearchQuery context) {}
