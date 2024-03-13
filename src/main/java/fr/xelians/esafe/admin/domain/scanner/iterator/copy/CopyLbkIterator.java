/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.copy;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class CopyLbkIterator extends AllLbkIterator {

  public CopyLbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(OperationSe operationSe, String[] tokens) {
    // TODO Optimise by removing tokens 3 and 4 that are not necessary
    if (operationSe.getType() == OperationType.INGEST_ARCHIVE
        || operationSe.getType() == OperationType.INGEST_HOLDING
        || operationSe.getType() == OperationType.INGEST_FILING) {

      operationSe.addAction(StorageAction.create(tokens));
    }
  }

  @Override
  public void actionUpdate(OperationSe operationSe, String[] tokens) {
    // We only need to know existing archives (i.e. created and not yet deleted archive) so update
    // is not necessary
  }

  @Override
  public void actionDelete(OperationSe operationSe, String[] tokens) {
    if (operationSe.getType() == OperationType.ELIMINATE_ARCHIVE) {
      operationSe.addAction(StorageAction.create(tokens));
    }
  }
}
