/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import jakarta.validation.constraints.Size;

public record ExportQuery(
    @JsonProperty("dipExportType") DipExportType dipExportType,
    @JsonProperty("dataObjectVersionToExport") DataObjectVersionToExport dataObjectVersionToExport,
    @JsonProperty("transferWithLogBookLFC") boolean transferWithLogBookLFC,
    @JsonProperty("dipRequestParameters") DipRequestParameters dipRequestParameters,
    @JsonProperty("dslRequest") SearchQuery searchQuery,
    @JsonProperty("maxSizeThreshold") long maxSizeThreshold,
    @JsonProperty("sedaVersion") @Size(max = 1024) String sedaVersion) {}
