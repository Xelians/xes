/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.iterator.unit;

import fr.xelians.esafe.admin.domain.scanner.AllLbkIterator;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.service.StorageService;
import java.util.List;

public class UnitLbkIterator extends AllLbkIterator {

  // Fetch all operations (needed for indexing operations)
  // Process only add/delete actions for unit type only (needed for indexing archive)
  // Keep only id (type is always unit) - needed for add/remove in StorageActionSet

  public UnitLbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    super(tenantDb, offers, storageService);
  }

  @Override
  public void actionCreate(OperationSe operationSe, String[] tokens) {
    if (StorageObjectType.uni.name().equals(tokens[2])) {
      operationSe.addAction(StorageAction.create(tokens));
    }
  }

  @Override
  public void actionUpdate(OperationSe operationSe, String[] tokens) {
    // We only need to know existing archive units (i.e. created and not yet deleted archive unit)
    // so update is not necessary
  }

  @Override
  public void actionDelete(OperationSe operationSe, String[] tokens) {
    if (StorageObjectType.uni.name().equals(tokens[2])) {
      operationSe.addAction(StorageAction.create(tokens));
    }
  }
}
