/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

public enum BinaryQualifier {
  BinaryMaster,
  Dissemination,
  Thumbnail,
  TextContent;

  public static boolean isValid(String str) {
    return BinaryMaster.toString().equals(str)
        || Dissemination.toString().equals(str)
        || Thumbnail.toString().equals(str)
        || TextContent.toString().equals(str);
  }
}
