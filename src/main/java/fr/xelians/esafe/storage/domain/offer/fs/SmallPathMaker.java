/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageObjectType;

public class SmallPathMaker implements PathMaker {

  public static final SmallPathMaker INSTANCE = new SmallPathMaker();

  private SmallPathMaker() {}

  @Override
  public String makePath(StorageObjectType type, long id) {
    String str = "0" + id;
    int len = str.length();
    return new StringBuilder()
        .append(str.charAt(len - 1))
        .append(str.charAt(len - 2))
        .append("/")
        .append(id)
        .append(".")
        .append(type)
        .toString();
  }
}
