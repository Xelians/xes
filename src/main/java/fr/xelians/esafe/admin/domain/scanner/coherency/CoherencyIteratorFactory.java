/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.coherency;

import fr.xelians.esafe.admin.domain.scanner.*;
import fr.xelians.esafe.admin.domain.scanner.iterator.check.CheckDbIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.check.CheckLbkIterator;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public record CoherencyIteratorFactory(
    StorageService storageService, OperationService operationService) implements IteratorFactory {

  @Override
  public LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers) {
    return new CheckLbkIterator(tenantDb, offers, storageService);
  }

  @Override
  public DbIterator createDbIterator(Long tenant, long maxOperationId) {
    return new CheckDbIterator(tenant, maxOperationId, operationService);
  }
}
