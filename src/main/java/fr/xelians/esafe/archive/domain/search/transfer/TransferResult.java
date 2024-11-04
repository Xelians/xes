/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.search.transfer;

import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public record TransferResult<T>(
    List<T> results,
    DataObjectVersionToExport dataObjectVersionToExport,
    boolean transferWithLogBookLFC,
    TransferRequestParameters transferRequestParameters,
    String sedaVersion,
    long maxSize) {}
