/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.export;

import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import fr.xelians.esafe.archive.domain.search.export.DipExportType;
import fr.xelians.esafe.archive.domain.search.export.DipRequestParameters;

public record ExportConfig(
    DipExportType dipExportType,
    DataObjectVersionToExport dataObjectVersionToExport,
    boolean transferWithLogBookLFC,
    DipRequestParameters dipRequestParameters,
    String sedaVersion) {}
