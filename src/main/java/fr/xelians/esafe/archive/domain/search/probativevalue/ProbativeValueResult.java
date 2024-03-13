/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.probativevalue;

import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import java.util.List;
import java.util.Set;

public record ProbativeValueResult<T>(
    List<T> results, Set<BinaryQualifier> usages, String version) {}
