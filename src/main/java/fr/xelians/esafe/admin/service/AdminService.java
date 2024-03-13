/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.service;

import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ID_MUST_BE_NOT_NULL = "id must be not null";

  private final OperationService operationService;
  private final StorageService storageService;
  private final TenantService tenantService;
  private final IndexAdminService indexAdminService;

  public InputStream getReportStream(Long tenant, Long id) throws IOException {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(id, ID_MUST_BE_NOT_NULL);

    TenantDb tenantDb = tenantService.getTenantDb(tenant);
    List<String> offers = tenantDb.getStorageOffers();
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      return storageDao.getReportStream(tenant, offers, id);
    }
  }

  public long getActionsSize(TenantDb tenantDb, List<String> offers, Long maxOperationId)
      throws IOException {
    return indexAdminService.getOperationsSize(tenantDb, offers, maxOperationId) * 4;
  }
}
