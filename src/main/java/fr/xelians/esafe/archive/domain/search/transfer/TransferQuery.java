/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import fr.xelians.esafe.archive.domain.search.search.SearchQuery;
import jakarta.validation.constraints.Size;

/*
 * @author Emmanuel Deviller
 */
public record TransferQuery(
    @JsonProperty("dataObjectVersionToExport") DataObjectVersionToExport dataObjectVersionToExport,
    @JsonProperty("transferWithLogBookLFC") boolean transferWithLogBookLFC,
    @JsonProperty("transferRequestParameters") TransferRequestParameters transferRequestParameters,
    @JsonProperty("dslRequest") SearchQuery searchQuery,
    @JsonProperty("maxSizeThreshold") long maxSizeThreshold,
    @JsonProperty("sedaVersion") @Size(max = 1024) String sedaVersion) {}
