/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

public record BinaryVersion(BinaryQualifier qualifier, Integer version) {
  @Override
  public String toString() {
    return version == null ? qualifier.toString() : qualifier + "_" + version;
  }
}
