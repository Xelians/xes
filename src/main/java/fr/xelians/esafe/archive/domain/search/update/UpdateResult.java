/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.update;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record UpdateResult<T>(
    String context, Integer from, Integer size, List<T> results, JsonNode jsonPatch) {}
