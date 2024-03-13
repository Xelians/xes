/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner;

import fr.xelians.esafe.organization.entity.TenantDb;
import java.util.List;

public interface IteratorFactory {

  LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers);

  DbIterator createDbIterator(Long tenant, long maxOperationId);
}
