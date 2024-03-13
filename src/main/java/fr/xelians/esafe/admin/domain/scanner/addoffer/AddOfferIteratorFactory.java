/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.addoffer;

import fr.xelians.esafe.admin.domain.scanner.DbIterator;
import fr.xelians.esafe.admin.domain.scanner.IteratorFactory;
import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.copy.CopyDbIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.copy.CopyLbkIterator;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public record AddOfferIteratorFactory(
    StorageService storageService, OperationService operationService) implements IteratorFactory {

  public LbkIterator createLbkIterator(TenantDb tenantDb, List<String> offers) {
    return new CopyLbkIterator(tenantDb, offers, storageService);
  }

  public DbIterator createDbIterator(Long tenant, long maxOperationId) {
    return new CopyDbIterator(tenant, maxOperationId, operationService);
  }
}
