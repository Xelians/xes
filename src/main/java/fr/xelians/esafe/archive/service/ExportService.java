/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.service;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.JsonProcessingException;
import fr.xelians.esafe.archive.domain.export.ExportConfig;
import fr.xelians.esafe.archive.domain.export.Exporter;
import fr.xelians.esafe.archive.domain.export.sedav2.Sedav2Exporter;
import fr.xelians.esafe.archive.domain.ingest.OntologyMapper;
import fr.xelians.esafe.archive.domain.search.ArchiveUnitQueryFactory;
import fr.xelians.esafe.archive.domain.search.export.DataObjectVersionToExport;
import fr.xelians.esafe.archive.domain.search.export.ExportParser;
import fr.xelians.esafe.archive.domain.search.export.ExportQuery;
import fr.xelians.esafe.archive.domain.search.export.ExportResult;
import fr.xelians.esafe.archive.domain.unit.*;
import fr.xelians.esafe.archive.domain.unit.object.BinaryQualifier;
import fr.xelians.esafe.archive.domain.unit.object.ObjectVersion;
import fr.xelians.esafe.archive.domain.unit.object.Qualifiers;
import fr.xelians.esafe.archive.task.ExportTask;
import fr.xelians.esafe.common.exception.functional.BadRequestException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Context;
import fr.xelians.esafe.common.utils.JsonUtils;
import fr.xelians.esafe.common.utils.UnitUtils;
import fr.xelians.esafe.operation.domain.OperationFactory;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.organization.service.TenantService;
import fr.xelians.esafe.processing.ProcessingService;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.referential.service.AccessContractService;
import fr.xelians.esafe.referential.service.OntologyService;
import fr.xelians.esafe.search.service.SearchEngineService;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class ExportService {

  public static final String TENANT_MUST_BE_NOT_NULL = "Tenant must be not null";
  public static final String ACCESS_CONTRACT_MUST_BE_NOT_NULL = "Access contract must be not null";
  public static final String QUERY_MUST_BE_NOT_NULL = "Query must be not null";
  public static final String USER_MUST_BE_NOT_NULL = "User must be not null";

  private final ProcessingService processingService;
  private final OperationService operationService;
  private final SearchEngineService searchEngineService;
  private final StorageService storageService;
  private final AccessContractService accessContractService;
  private final OntologyService ontologyService;
  private final TenantService tenantService;

  @Value("${app.dipexport.maxSize:O}")
  private long maxSize;

  public Long export(
      Long tenant, String accessContract, ExportQuery exportQuery, String user, String app) {
    Assert.notNull(tenant, TENANT_MUST_BE_NOT_NULL);
    Assert.notNull(accessContract, ACCESS_CONTRACT_MUST_BE_NOT_NULL);
    Assert.notNull(exportQuery, QUERY_MUST_BE_NOT_NULL);
    Assert.notNull(user, USER_MUST_BE_NOT_NULL);

    String query = JsonService.toString(exportQuery);
    OperationDb operation =
        OperationFactory.exportArchiveOp(tenant, user, app, accessContract, query);
    operation = operationService.save(operation);

    processingService.submit(new ExportTask(operation, this));
    return operation.getId();
  }

  public Path check(OperationDb operation, TenantDb tenantDb) {

    Long tenant = tenantDb.getId();

    // Get the accessContact and the mapper
    AccessContractDb accessContract =
        accessContractService.getEntity(tenant, operation.getProperty01());
    OntologyMapper ontologyMapper = ontologyService.createMapper(tenant);

    // Search Archive Units to update
    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      // Query
      String query = operation.getProperty02();
      ExportResult<ArchiveUnit> result = search(tenant, accessContract, ontologyMapper, query);

      Path tmpDipPath = Workspace.createTempFile(operation);

      List<String> offers = tenantDb.getStorageOffers();
      List<ArchiveUnit> archiveUnits = new ArrayList<>();
      List<ArchiveUnit> selectedUnits = result.results();

      // Group selected Archive Units by operation id
      Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

      // Export archive units by operation id
      for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
        Long opId = entry.getKey();
        List<ArchiveUnit> indexedUnits = entry.getValue();

        // From storage offers read units with operationId and map by unit id.
        Map<Long, ArchiveUnit> storedUnitMap =
            UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

        // Loop on indexed archive units
        for (ArchiveUnit indexedUnit : indexedUnits) {
          Long archiveUnitId = indexedUnit.getId();

          // Check storage and index coherency
          ArchiveUnit storedUnit = storedUnitMap.get(archiveUnitId);
          UnitUtils.checkVersion(indexedUnit, storedUnit);
          archiveUnits.add(indexedUnit);
        }
      }

      // Check Dip Size
      checkMaxSize(archiveUnits, result.dataObjectVersionToExport());

      // Export the binary to the tmp path
      ExportConfig exportConfig =
          new ExportConfig(
              result.dipExportType(),
              result.dataObjectVersionToExport(),
              result.transferWithLogBookLFC(),
              result.dipRequestParameters(),
              result.sedaVersion());

      Context context =
          new Context(operation, tenantDb, accessContract, ontologyMapper, null, tmpDipPath);
      doExport(context, exportConfig, archiveUnits, storageDao);

      return tmpDipPath;
    } catch (IOException ex) {
      throw new InternalException(ex);
    }
  }

  private ExportResult<ArchiveUnit> search(
      Long tenant, AccessContractDb accessContract, OntologyMapper ontologyMapper, String query)
      throws IOException {

    ExportQuery exportQuery = ArchiveUnitQueryFactory.createExportQuery(query);
    ExportParser exportParser = ExportParser.create(tenant, accessContract, ontologyMapper);

    try {
      // TODO Deals with maxSizeThreshold
      SearchRequest exportRequest = exportParser.createRequest(exportQuery.searchQuery());

      log.info("Update JSON  - request: {}", JsonUtils.toJson(exportRequest));
      SearchResponse<ArchiveUnit> response =
          searchEngineService.search(exportRequest, ArchiveUnit.class);

      // TODO check if result overflows
      List<ArchiveUnit> units = response.hits().hits().stream().map(Hit::source).toList();
      return new ExportResult<>(
          units,
          exportQuery.dipExportType(),
          exportQuery.dataObjectVersionToExport(),
          exportQuery.transferWithLogBookLFC(),
          exportQuery.dipRequestParameters(),
          exportQuery.sedaVersion(),
          maxSize);

    } catch (JsonProcessingException ex) {
      throw new BadRequestException(
          "Search request failed", String.format("Failed to parse query '%s'", exportQuery), ex);
    }
  }

  private void checkMaxSize(List<ArchiveUnit> units, DataObjectVersionToExport dov) {
    if (dov != null && dov.dataObjectVersions() != null && maxSize > 0) {
      long size = 0;

      for (ArchiveUnit unit : units) {
        for (BinaryQualifier bq : dov.dataObjectVersions()) {
          String qualifier = bq.toString();
          for (Qualifiers q : unit.getQualifiers()) {
            if (q.getQualifier().equals(qualifier)) {
              ObjectVersion objectVersion = ObjectVersion.getGreatestVersion(q.getVersions());
              size += objectVersion.getSize();
              if (size > maxSize) {
                throw new BadRequestException(
                    String.format(
                        "The export dip '%s' is greater than the allowed max size of '%s'",
                        size, maxSize));
              }
              break;
            }
          }
        }
      }
    }

    //      for (ArchiveUnit unit : units) {
    //        DataObject dataObject = unit.getDataObject();
    //        if (dataObject != null) {
    //          for (BinaryQualifier qualifier : qualifiers) {
    //            BinaryDataObject bdo = dataObject.getGreatestBinaryDataObject(qualifier);
    //            if (bdo != null) {
    //              size += bdo.getSize();
    //              if (size > maxSize) {
    //                throw new BadRequestException(
    //                    String.format(
    //                        "The export dip '%s' is greater than the allowed max size of '%s'",
    //                        size, maxSize));
    //              }
    //            }
    //          }
    //        }
    //      }
    //    }
  }

  private void doExport(
      Context context, ExportConfig exportConfig, List<ArchiveUnit> units, StorageDao storageDao)
      throws IOException {

    // Add the archive unit to its parent archive unit (if any)
    Map<Long, ArchiveUnit> unitMap = UnitUtils.mapById(units);

    List<ArchiveUnit> srcUnits = new ArrayList<>();

    for (ArchiveUnit unit : units) {
      ArchiveUnit parentUnit = unitMap.get(unit.getParentId());
      if (parentUnit == null) {
        srcUnits.add(unit);
      } else {
        parentUnit.getChildUnitMap().put(unit.getId().toString(), unit);
      }
    }

    TenantDb tenantDb = context.tenantDb();

    String version = exportConfig.sedaVersion();
    if (version != null && !version.toLowerCase().startsWith("json")) {
      // Seda exporter
      Exporter exporter =
          new Sedav2Exporter(
              tenantDb.getId(), tenantDb.getStorageOffers(), storageDao, exportConfig);
      exporter.export(srcUnits, context.path());
    } else {
      // TODO Json exporter
      throw new BadRequestException(String.format("Export '%s' is not implemented", version));
    }
  }

  public void commit(OperationDb operation, TenantDb tenantDb, Path tmpDipPath) {
    Long operationId = operation.getId();
    Long tenant = operation.getTenant();
    List<String> offers = tenantDb.getStorageOffers();

    List<StorageObject> storageObjects = new ArrayList<>();

    // Write Archive Units (.aus) from temporary file
    storageObjects.add(new PathStorageObject(tmpDipPath, operationId, StorageObjectType.dip, true));

    // Write Operation (This allows to restore operations from offers to database)
    byte[] ops = JsonService.toBytes(operation, JsonConfig.DEFAULT);
    storageObjects.add(new ByteStorageObject(ops, operationId, StorageObjectType.ope, true));

    try (StorageDao storageDao = storageService.createStorageDao(tenantDb)) {
      // Write .aus and .ope to offers
      storageDao.putStorageObjects(tenant, offers, storageObjects);
    } catch (IOException ex) {
      // Rollback as much as possible
      storageService.deleteObjectsQuietly(offers, tenant, storageObjects);
      throw new InternalException(ex);
    }
  }
}
