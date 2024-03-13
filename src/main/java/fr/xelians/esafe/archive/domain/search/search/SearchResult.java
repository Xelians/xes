/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.search.domain.dsl.bucket.Bucket;
import java.util.List;
import java.util.Map;

public record SearchResult<T>(
    @JsonProperty("httpCode") int httpCode,
    @JsonProperty("$hits") Hits hits,
    @JsonProperty("$results") List<T> results,
    @JsonProperty("$facetResults") Map<String, List<Bucket>> facets,
    @JsonProperty("$context") SearchQuery context) {}
