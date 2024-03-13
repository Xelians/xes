/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.ingest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicOntologyMap implements OntologyMap {

  private final Map<String, String> mappings;

  public BasicOntologyMap(List<Mapping> mappings) {
    this.mappings = mappings.stream().collect(Collectors.toMap(Mapping::src, Mapping::dst));
  }

  public BasicOntologyMap(Map<String, String> mappings) {
    this.mappings = mappings;
  }

  @Override
  public String get(String src) {
    return mappings.get(src);
  }

  @Override
  public boolean containsKey(String src) {
    return mappings.containsKey(src);
  }
}
