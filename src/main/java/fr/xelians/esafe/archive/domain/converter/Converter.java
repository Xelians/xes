/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.converter;

import com.fasterxml.jackson.databind.JsonNode;

public interface Converter {

  JsonNode convert(JsonNode srcNode);
}
