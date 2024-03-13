/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.service;

import static fr.xelians.esafe.common.utils.ExceptionsUtils.format;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.ThreadUtils;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexService {

  private static final ThreadPoolExecutor INDEX_POOL = ThreadUtils.blockingPool(8, 8);

  private final SearchService searchService;
  private final OperationService operationService;
  private final TenantService tenantService;
  private final StorageService storageService;
  private final LogbookService logbookService;

  public void indexOperation(OperationDb operation) {
    try {
      Long tenant = operation.getTenant();
      TenantDb tenantDb = tenantService.getTenantDb(tenant);
      List<String> offers = tenantDb.getStorageOffers();

      try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
        List<Long> ids =
            operation.getActions().stream()
                .map(StorageAction::create)
                .map(StorageObjectId::getId)
                .toList();
        indexArchives(storageDao, tenant, offers, ids);

        // Index Operation in Search Engine
        logbookService.index(operation);

        // Save Operation to Db
        operationService.unlockAndSave(operation, OperationStatus.OK, "Index done successfully");
      }
    } catch (Exception ex) {
      EsafeException e = ex instanceof EsafeException esafeEx ? esafeEx : new InternalException(ex);
      log.error(format("Index archive unit failed", e, operation), ex);
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Failed to index archive units. Waiting for automatic retry.");
      operationService.save(operation);
    }
  }

  public void indexArchives(
      StorageDao storageDao, Long tenant, List<String> offers, List<Long> opIds)
      throws IOException, InterruptedException, ExecutionException {

    List<Future<?>> futures = new ArrayList<>();
    try {
      for (List<Long> partOpIds : ListUtils.partition(opIds, 8)) {
        List<ArchiveUnit> archiveUnits = storageDao.getArchiveUnits(tenant, offers, partOpIds);
        for (List<ArchiveUnit> partUnits :
            ListUtils.partition(archiveUnits, SearchService.ARCHIVE_UNIT_BULK_SIZE)) {
          futures.add(INDEX_POOL.submit(() -> this.bulkIndex(partUnits)));
        }
      }
      ThreadUtils.joinFutures(futures, 5, TimeUnit.MINUTES);
    } catch (TimeoutException ex) {
      futures.forEach(future -> future.cancel(false));
      throw new InternalException(
          "Index archive in store operation batch failed", "Indexation exception", ex);
    }
  }

  @SneakyThrows
  private void bulkIndex(List<ArchiveUnit> archiveUnits) {
    searchService.bulkIndex(archiveUnits);
  }
}
