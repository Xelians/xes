/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.service;

import static fr.xelians.esafe.operation.domain.ActionType.CREATE;
import static fr.xelians.esafe.storage.domain.StorageObjectType.lbk;

import fr.xelians.esafe.admin.domain.scanner.LbkIterator;
import fr.xelians.esafe.admin.domain.scanner.iterator.securing.SecuringLbkIterator;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.logbook.service.LogbookService;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.entity.OperationSe;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.offer.StorageOffer;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecuringService {

  private final OperationService operationService;
  private final LogbookService logbookService;
  private final StorageService storageService;
  private final TenantService tenantService;

  @Transactional(rollbackFor = Exception.class)
  public long writeLbk(
      OperationDb operation, List<OperationDb> operations, List<String> offers, Hash hash) {

    long secureNum = -1;
    Long tenant = operation.getTenant();

    try {
      secureNum = storageService.getStorageLog(tenant).incSecureNumber();
      byte[] checksum = writeLbk(tenant, operations, secureNum, offers, hash);
      operations.forEach(operationService::secureAndsave);

      operation.setProperty01(String.valueOf(secureNum));
      operation.addAction(StorageAction.create(CREATE, secureNum, lbk, hash, checksum));
      operationService.save(operation);
      return secureNum;

    } catch (Exception ex) {
      operationService.unlockAndSave(
          operation, OperationStatus.FATAL, "Failed securing logbook operation");

      // Rollback objects on storage offers
      if (secureNum != -1) {
        storageService.deleteObjectQuietly(offers, operation.getTenant(), secureNum, lbk);
      }
      // Rollback transaction
      throw new InternalException(
          "Write operation log failed", "Failed securing logbook operation", ex);
    }
  }

  private byte[] writeLbk(
      Long tenant, List<OperationDb> operations, long secureNum, List<String> offers, Hash hash)
      throws IOException {

    byte[] checksum = null;
    for (String offer : offers) {
      StorageOffer storageOffer = storageService.getStorageOffer(offer);
      byte[] cs = storageOffer.writeLbk(tenant, operations, secureNum, hash);
      if (checksum == null) {
        checksum = cs;
      } else if (!Arrays.equals(checksum, cs)) {
        throw new InternalException(
            "Write operation log failed",
            String.format(
                "Failed securing logbook operation - tenant: %s - id: %s - checksums are different between storage offers- '%s' != '%s' ",
                tenant, secureNum, HashUtils.encodeHex(checksum), HashUtils.encodeHex(cs)));
      }
    }
    return checksum;
  }

  public void index(OperationDb operation, List<OperationSe> operations) {
    try {
      logbookService.bulkIndex(operations);
      logbookService.index(operation);
      operationService.unlockAndSave(
          operation, OperationStatus.OK, "Securing operations completed with success");

    } catch (Exception e) {
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Retry indexing secure operation");
      operationService.save(operation);
    }
  }

  public void deleteOpe(List<String> offers, List<OperationSe> operations) {
    for (OperationSe operation : operations) {
      storageService.deleteObjectQuietly(
          offers, operation.getTenant(), operation.getId(), StorageObjectType.ope);
    }
  }

  public void indexOperation(OperationDb operation) {
    TenantDb tenantDb = tenantService.getTenantDb(operation.getTenant());
    List<String> offers = tenantDb.getStorageOffers();
    long lbkId = Long.parseLong(operation.getProperty01());

    List<OperationSe> ops = new ArrayList<>();
    try (LbkIterator it = new SecuringLbkIterator(tenantDb, storageService, lbkId)) {
      while (it.hasNext()) {
        ops.add(it.next());
      }
    } catch (Exception e) {
      operation.setStatus(OperationStatus.RETRY_INDEX);
      operation.setMessage("Retry indexing secure operation");
      operationService.save(operation);
      return;
    }

    index(operation, ops);
    deleteOpe(offers, ops);
  }
}
