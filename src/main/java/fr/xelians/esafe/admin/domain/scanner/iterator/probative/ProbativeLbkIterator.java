/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.probative;

import fr.xelians.esafe.admin.domain.scanner.OneLbkIterator;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class ProbativeLbkIterator extends OneLbkIterator {

  public ProbativeLbkIterator(TenantDb tenantDb, StorageService storageService, long logId) {
    super(tenantDb, tenantDb.getStorageOffers(), storageService, logId);
  }

  public ProbativeLbkIterator(
      TenantDb tenantDb, List<String> offers, StorageService storageService, long logId) {
    super(tenantDb, offers, storageService, logId);
  }

  @Override
  public void actionCreate(OperationSe operationSe, String[] tokens) {
    if (operationSe.getType() == OperationType.INGEST_ARCHIVE) {
      StorageAction storageAction = StorageAction.create(tokens);
      if (storageAction.getType() == StorageObjectType.atr) {
        operationSe.addAction(storageAction);
      }
    }
  }

  @Override
  public void actionUpdate(OperationSe operationSe, String[] tokens) {
    // TODO Deal with update binary version
  }

  @Override
  public void actionDelete(OperationSe operationSe, String[] tokens) {}
}
