/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record DataObjectVersionToExport(
    @JsonProperty("dataObjectVersions") @Size(max = 4) Set<BinaryQualifier> dataObjectVersions) {}
