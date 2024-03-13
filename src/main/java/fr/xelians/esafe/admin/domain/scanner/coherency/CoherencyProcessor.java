/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.coherency;

import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.storage.domain.hashset.StorageObjectSet;

public class CoherencyProcessor implements OperationProcessor {

  private final StorageObjectSet storageObjectSet;

  public CoherencyProcessor(StorageObjectSet storageObjectSet) {
    this.storageObjectSet = storageObjectSet;
  }

  @Override
  public void process(OperationSe operation) {
    OperationType type = operation.getType();
    if (type == OperationType.INGEST_ARCHIVE
        || type == OperationType.INGEST_HOLDING
        || type == OperationType.INGEST_FILING
        || type == OperationType.UPDATE_ARCHIVE
        || type == OperationType.UPDATE_ARCHIVE_RULES
        || type == OperationType.RECLASSIFY_ARCHIVE) {

      // Add all actions that modify the storage object (file) on the storage offers
      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getActionType() == ActionType.CREATE
            || storageAction.getActionType() == ActionType.UPDATE) {
          storageObjectSet.add(
              storageAction.getId(),
              storageAction.getType(),
              storageAction.getHash(),
              storageAction.getChecksum());
        }
      }
    } else if (type == OperationType.ELIMINATE_ARCHIVE) {
      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getActionType() == ActionType.DELETE) {
          // Remove action that delete the whole storage object (file) on the storage offers
          storageObjectSet.remove(storageAction.getId(), storageAction.getType());
        } else if (storageAction.getActionType() == ActionType.UPDATE) {
          // Add action that ont delete some part of the storage object (file) on the storage offers
          storageObjectSet.add(
              storageAction.getId(),
              storageAction.getType(),
              storageAction.getHash(),
              storageAction.getChecksum());
        }
      }
    }
  }

  @Override
  public void finish() {
    // Nothing to do here
  }
}
