package fr.xelians.esafe.search.domain.dsl.bucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Facet(
    @JsonProperty("name") String name, @JsonProperty("buckets") List<Bucket> buckets) {}
