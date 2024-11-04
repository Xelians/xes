/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.elimination;

import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.admin.domain.report.ReportStatus;
import fr.xelians.esafe.admin.domain.report.ReportType;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang.BooleanUtils;

/*
 * @author Emmanuel Deviller
 */
public class TransferEliminator extends Eliminator {

  public TransferEliminator(
      SearchService searchService, StorageService storageService, TenantDb tenantDb) {
    this(searchService, storageService, tenantDb, null, null, null);
  }

  public TransferEliminator(
      SearchService searchService,
      StorageService storageService,
      TenantDb tenantDb,
      AccessContractDb accessContract,
      Path path,
      List<ArchiveUnit> selectedUnits) {
    super(searchService, storageService, tenantDb, accessContract, path, selectedUnits);
  }

  @Override
  protected void checkUnit(ArchiveUnit unit) {
    if (BooleanUtils.isNotTrue(unit.getTransferred())) {
      throw new BadRequestException(
          String.format("Cannot eliminate '%s' a not transferred archive", unit.getId()));
    }
  }

  @Override
  protected ArchiveReporter createReporter(Path reportPath, OperationDb operation)
      throws IOException {
    return new ArchiveReporter(ReportType.TRANSFER, ReportStatus.OK, operation, reportPath);
  }
}
