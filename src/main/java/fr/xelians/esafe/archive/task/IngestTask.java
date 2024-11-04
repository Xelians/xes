/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.domain.ingest.AbstractManifestParser;
import fr.xelians.esafe.archive.domain.ingest.sedav2.Sedav2Parser;
import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.ManagementMetadata;
import fr.xelians.esafe.archive.domain.unit.object.DataObjectGroup;
import fr.xelians.esafe.archive.service.IngestService;
import fr.xelians.esafe.common.task.CommitIndexTask;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.IngestContractDb;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public class IngestTask extends CommitIndexTask {

  private final IngestService ingestService;
  private AbstractManifestParser parser;
  private TenantDb tenantDb;

  public IngestTask(OperationDb operation, IngestService ingestService) {
    super(operation, ingestService.getOperationService(), ingestService.getTenantService());
    this.ingestService = ingestService;
    this.tenantDb = tenantService.getTenantDb(operation.getTenant());
  }

  @Override
  public void check() {
    parser = new Sedav2Parser(ingestService, operation);
    ingestService.check(parser, operation, tenantDb);
  }

  // Commit SIP
  @Override
  public void commit() {
    List<ArchiveUnit> archiveUnits = parser.getArchiveUnits();
    ArchiveTransfer archiveTransfer = parser.getArchiveTransfer();
    ManagementMetadata managementMetadata = parser.getManagementMetadata();
    List<DataObjectGroup> dataObjectGroups = parser.getDataObjectGroups();
    IngestContractDb ingestContractDb = parser.getIngestContract();

    ingestService.commit(
        operation,
        tenantDb,
        archiveUnits,
        archiveTransfer,
        managementMetadata,
        dataObjectGroups,
        ingestContractDb);
  }

  // The index method must be idempotent
  @Override
  public void index() {
    ingestService.index(operation, parser.getArchiveUnits());
  }

  //  private void setAtrInfo(ArchiveTransfer atr) {
  //    AtrInfo atrInfo = atr == null ? new AtrInfo() : new AtrInfo(atr);
  //    String jsonInfo = JsonService.toString(atrInfo);
  //    operation.setProperty01(jsonInfo);
  //  }
}
