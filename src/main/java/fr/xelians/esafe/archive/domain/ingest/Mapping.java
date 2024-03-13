/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Mapping(@JsonProperty("Src") String src, @JsonProperty("Dst") String dst) {}
