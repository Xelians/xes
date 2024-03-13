/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.referential.entity;

import fr.xelians.esafe.common.entity.database.BaseDb;

public interface ReferentialDb extends BaseDb {

  Long getId();

  void setId(Long id);

  String getIdentifier();

  void setIdentifier(String identifier);

  Long getTenant();

  void setTenant(Long tenant);
}
