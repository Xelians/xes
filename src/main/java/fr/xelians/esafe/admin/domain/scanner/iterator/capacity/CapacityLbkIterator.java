/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.capacity;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class CapacityLbkIterator extends AllLbkIterator {

  public CapacityLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(OperationSe operationSe, String[] tokens) {
    operationSe.addAction(StorageAction.create(tokens));
  }

  @Override
  public void actionUpdate(OperationSe operationSe, String[] tokens) {
    // We only need to know the max capacity so processing update is not necessary
  }

  @Override
  public void actionDelete(OperationSe operationSe, String[] tokens) {
    // We only need to know the max capacity hence we should not process delete
  }
}
