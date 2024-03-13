/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.task;

import fr.xelians.esafe.archive.domain.ingest.AbstractManifestParser;
import fr.xelians.esafe.archive.domain.ingest.sedav2.AtrInfo;
import fr.xelians.esafe.archive.domain.ingest.sedav2.Sedav2Parser;
import fr.xelians.esafe.archive.domain.unit.ArchiveTransfer;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.domain.unit.object.DataObjectGroup;
import fr.xelians.esafe.archive.service.IngestService;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.task.CommitIndexTask;
import fr.xelians.esafe.common.utils.Perfs;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.IngestContractDb;
import java.util.List;

public class IngestionTask extends CommitIndexTask {

  private final IngestService ingestService;
  private AbstractManifestParser parser;
  private final Perfs perf;

  public IngestionTask(OperationDb operation, IngestService ingestService) {
    super(operation, ingestService.getOperationService(), ingestService.getTenantService());
    this.ingestService = ingestService;
    this.perf = Perfs.start();
  }

  @Override
  public void check() {
    perf.log("Ingest Queue " + operation.getId());
    perf.reset();

    try {
      parser = new Sedav2Parser(ingestService, operation);
      ingestService.check(parser, operation);
    } catch (Exception ex) {
      updateOperation(parser.getArchiveTransfert());
      throw ex;
    }

    perf.log("Ingest Check " + operation.getId());
  }

  // Commit SIP
  @Override
  public void commit() {
    perf.reset();

    TenantDb tenantDb = tenantService.getTenantDb(operation.getTenant());
    List<ArchiveUnit> archiveUnits = parser.getArchiveUnits();
    ArchiveTransfer archiveTransfert = parser.getArchiveTransfert();
    List<DataObjectGroup> dataObjectGroups = parser.getDataObjectGroups();
    IngestContractDb ingestContractDb = parser.getIngestContract();

    try {
      ingestService.commit(
          operation, tenantDb, archiveUnits, archiveTransfert, dataObjectGroups, ingestContractDb);
    } catch (Exception ex) {
      updateOperation(archiveTransfert);
      throw ex;
    }

    perf.log("Ingest Commit " + operation.getId());
  }

  // The index method must be idempotent
  @Override
  public void index() {
    perf.reset();

    ingestService.index(operation, parser.getArchiveUnits());

    perf.log("Ingest Index " + operation.getId());
  }

  private void updateOperation(ArchiveTransfer atr) {
    AtrInfo atrInfo = atr == null ? new AtrInfo() : new AtrInfo(atr);
    String jsonInfo = JsonService.toString(atrInfo);
    operation.setProperty01(jsonInfo);
  }
}
