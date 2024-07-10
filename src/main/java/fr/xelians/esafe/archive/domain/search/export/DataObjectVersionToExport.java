/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record DataObjectVersionToExport(
    @JsonProperty("dataObjectVersions") @Size(max = 4) Set<BinaryQualifier> dataObjectVersions) {}
