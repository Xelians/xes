/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.securing;

import fr.xelians.esafe.admin.domain.scanner.OneLbkIterator;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class SecuringLbkIterator extends OneLbkIterator {

  public SecuringLbkIterator(TenantDb tenantDb, StorageService storageService, long logId) {
    super(tenantDb, tenantDb.getStorageOffers(), storageService, logId);
  }

  public SecuringLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService, long logId) {
    super(tenantDb, offers, storageService, logId);
  }

  @Override
  public void actionCreate(OperationSe operationSe, String[] tokens) {
    // Nothing to do
  }

  @Override
  public void actionUpdate(OperationSe operationSe, String[] tokens) {
    // Nothing to do
  }

  @Override
  public void actionDelete(OperationSe operationSe, String[] tokens) {
    // Nothing to do
  }
}
