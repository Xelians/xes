/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Hits(
    @JsonProperty("offset") long offset,
    @JsonProperty("limit") int limit,
    @JsonProperty("size") long size,
    @JsonProperty("total") Long total) {}
