/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner.reindex;

import fr.xelians.esafe.admin.domain.scanner.OperationProcessor;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.hashset.IdSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReindexSearchIndexProcessor implements OperationProcessor {

  private final IdSet idSet;

  public static final int BULK_INDEX_SIZE = 5000;

  private final LogbookService logbookService;
  private final List<OperationSe> operations;

  public ReindexSearchIndexProcessor(IdSet idSet, LogbookService logbookService) {
    this.idSet = idSet;
    this.logbookService = logbookService;
    this.operations = new ArrayList<>();
  }

  @Override
  public void process(OperationSe operation) throws IOException {
    OperationType type = operation.getType();
    if (type == OperationType.INGEST_ARCHIVE
        || type == OperationType.INGEST_HOLDING
        || type == OperationType.INGEST_FILING) {

      // Index Archive Units
      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getType() == StorageObjectType.uni) {
          idSet.add(storageAction.getId());
          break; // There is only 1 unit in an ingest operation
        }
      }
    } else if (type == OperationType.ELIMINATE_ARCHIVE) {
      for (StorageAction storageAction : operation.getStorageActions()) {
        if (storageAction.getActionType() == ActionType.DELETE
            && storageAction.getType() == StorageObjectType.uni) {
          idSet.remove(storageAction.getId());
        }
      }
    }

    // Index Operations
    operations.add(operation);
    if (operations.size() >= BULK_INDEX_SIZE) {
      logbookService.bulkIndex(operations);
      operations.clear();
    }
  }

  @Override
  public void finish() throws IOException {
    if (!operations.isEmpty()) {
      logbookService.bulkIndex(operations);
      operations.clear();
    }
  }
}
