/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.search.domain.dsl.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record Facet(
    @JsonProperty("name") String name, @JsonProperty("buckets") List<Bucket> buckets) {}
