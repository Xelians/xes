/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.export;

import java.util.List;

public record ExportResult<T>(
    List<T> results,
    DipExportType dipExportType,
    DataObjectVersionToExport dataObjectVersionToExport,
    boolean transferWithLogBookLFC,
    DipRequestParameters dipRequestParameters,
    String sedaVersion,
    long maxSize) {}
