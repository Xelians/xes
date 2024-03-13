/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageObjectType;

public class LargePathMaker implements PathMaker {

  public static final LargePathMaker INSTANCE = new LargePathMaker();

  private LargePathMaker() {}

  @Override
  public String makePath(StorageObjectType type, long id) {
    String str = "00000" + id;
    int len = str.length();
    return ""
        + str.charAt(len - 1)
        + str.charAt(len - 2)
        + "/"
        + str.charAt(len - 3)
        + str.charAt(len - 4)
        + "/"
        + str.charAt(len - 5)
        + str.charAt(len - 6)
        + "/"
        + id
        + "."
        + type; // keep the first ""
  }
}
