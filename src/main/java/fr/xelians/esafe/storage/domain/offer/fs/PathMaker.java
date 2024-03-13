/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import fr.xelians.esafe.storage.domain.StorageObjectType;

public interface PathMaker {

  String makePath(StorageObjectType type, long id);
}
