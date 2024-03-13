/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
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
