/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.archive.domain.elimination;

import static fr.xelians.esafe.common.utils.Hash.MD5;
import static fr.xelians.esafe.operation.domain.ActionType.DELETE;
import static fr.xelians.esafe.operation.domain.ActionType.UPDATE;

import com.fasterxml.jackson.databind.SequenceWriter;
import fr.xelians.esafe.admin.domain.report.ArchiveReporter;
import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.archive.service.SearchService;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.json.JsonConfig;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.CollUtils;
import fr.xelians.esafe.common.utils.ListIterator;
import fr.xelians.esafe.common.utils.UnitUtils;
import fr.xelians.esafe.operation.domain.ActionType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.referential.entity.AccessContractDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.collections4.ListUtils;

/*
 * @author Emmanuel Deviller
 */
@Getter
public abstract class Eliminator {

  private static final byte[] ZERO_BYTES = new byte[] {0};

  private final List<ArchiveUnit> selectedUnits;
  private final SearchService searchService;
  private final StorageService storageService;

  private final Long tenant;
  private final List<String> offers;
  private final Path path;
  private final AccessContractDb accessContract;

  protected Eliminator(
      SearchService searchService,
      StorageService storageService,
      TenantDb tenantDb,
      AccessContractDb accessContract,
      Path path,
      List<ArchiveUnit> selectedUnits) {
    this.searchService = searchService;
    this.storageService = storageService;
    this.tenant = tenantDb.getId();
    this.offers = tenantDb.getStorageOffers();
    this.accessContract = accessContract;
    this.path = path;
    this.selectedUnits = selectedUnits;
  }

  public void check(StorageDao storageDao) throws IOException {
    List<ArchiveUnit> units = checkSelected(storageDao);
    checkChildren(storageDao, units);
  }

  private List<ArchiveUnit> checkSelected(StorageDao storageDao) throws IOException {
    List<ArchiveUnit> units = new ArrayList<>();

    // Group selected Archive Units by operation id
    Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(selectedUnits);

    // Eliminate top archive units by operation id
    for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
      Long opId = entry.getKey();

      // Read from storage offers archive units with unit.operationId and group them by archive
      // unit id. Check storage and index coherency. Could be optional
      Map<Long, ArchiveUnit> storedUnitMap =
          UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

      // Loop on indexed archive units
      for (ArchiveUnit selectedUnit : entry.getValue()) {
        Long selectedUnitId = selectedUnit.getId();

        // Check storage and index coherency. Could be optional
        ArchiveUnit storedUnit = storedUnitMap.get(selectedUnitId);
        UnitUtils.checkVersion(selectedUnit, storedUnit);

        checkUnit(selectedUnit);
        units.add(selectedUnit);
      }
    }
    return units;
  }

  private void checkChildren(StorageDao storageDao, List<ArchiveUnit> parentUnits)
      throws IOException {

    // Eliminate children archive units by operation id
    Map<Long, ArchiveUnit> topUnitMap = UnitUtils.mapById(parentUnits);

    String pitId = null;
    try (SequenceWriter writer = JsonService.createSequenceWriter(path)) {
      writer.writeAll(parentUnits);
      pitId = searchService.openPointInTime();

      // We limit the search to 1000 terms. Elastic search limit is 65,536
      for (List<ArchiveUnit> partList : ListUtils.partition(parentUnits, 1000)) {
        List<Long> parentIds = partList.stream().map(ArchiveUnit::getId).toList();
        Stream<ArchiveUnit> stream =
            searchService
                .searchAllChildrenStream(tenant, accessContract, parentIds, pitId)
                .filter(au -> !topUnitMap.containsKey(au.getId()));
        CollUtils.chunk(stream, 10000)
            .forEach(childUnits -> doCheckChildren(storageDao, writer, childUnits));
      }
    } finally {
      searchService.closePointInTime(pitId);
    }
  }

  @SneakyThrows
  private void doCheckChildren(
      StorageDao storageDao, SequenceWriter writer, List<ArchiveUnit> childUnits) {

    // Group Archive Units by operation id
    Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(childUnits);

    // Eliminate archive units by operation id
    for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
      Long opId = entry.getKey();
      List<ArchiveUnit> groupedChildUnits = entry.getValue();

      // Read from storage offers archive units with unit.operationId and group them by archive unit
      // id
      // Check storage and index coherency. Could be optional
      Map<Long, ArchiveUnit> storedUnitMap =
          UnitUtils.mapById(storageDao.getArchiveUnits(tenant, offers, opId));

      // Loop on children archive units
      for (ArchiveUnit childUnit : groupedChildUnits) {

        // Check storage and index coherency. Could be optional
        ArchiveUnit storedUnit = storedUnitMap.get(childUnit.getId());
        UnitUtils.checkVersion(childUnit, storedUnit);

        // Process each child
        checkUnit(childUnit);
        writer.write(childUnit);
      }
    }
  }

  public void eliminate(OperationDb operation, InputStream ausStream, StorageDao storageDao)
      throws IOException {

    Long operationId = operation.getId();

    boolean isFirst = true;
    boolean needRefresh = false;
    long byteCount = 0;
    List<StorageObject> storageObjects = new ArrayList<>();

    // Reset Actions
    operation.resetActions();

    Path reportPath = Workspace.createTempFile(operation);
    try (ArchiveReporter reporter = createReporter(reportPath, operation)) {

      // Get updated Archive Units from stream
      // TODO Optimize by getting only relevant fields from archive unit
      Iterator<ArchiveUnit> iterator = JsonService.toArchiveUnitIterator(ausStream);
      Iterator<List<ArchiveUnit>> listIterator = ListIterator.iterator(iterator, 10000);
      while (listIterator.hasNext()) {
        List<ArchiveUnit> indexedArchiveUnits = listIterator.next();
        Map<Long, List<ArchiveUnit>> gmap = UnitUtils.groupByOpId(indexedArchiveUnits);

        // Write archive units by operation id
        for (Map.Entry<Long, List<ArchiveUnit>> entry : gmap.entrySet()) {
          Long opId = entry.getKey();
          List<ArchiveUnit> archiveUnits = entry.getValue();

          for (ArchiveUnit au : archiveUnits) {
            reporter.writeUnitWithObjects(au);
          }

          // Read from storage offers archive units with unit.operationId and group them by archive
          // unit id
          Map<Long, ArchiveUnit> storedUnits =
              getStoredUnits(opId, tenant, offers, archiveUnits, storageDao);

          if (storedUnits.isEmpty()) {
            deleteObjects(operation, offers, tenant, opId);
          } else {
            byte[] bytes = JsonService.collToBytes(storedUnits.values(), JsonConfig.DEFAULT);
            storageObjects.add(new ByteStorageObject(bytes, opId, StorageObjectType.uni));
            byteCount += bytes.length;

            if (byteCount > 256_000_000) {
              // Commit the created/modified units to offers and create actions
              storageDao
                  .putStorageObjects(tenant, offers, storageObjects)
                  .forEach(e -> operation.addAction(StorageAction.create(UPDATE, e)));
              storageObjects = new ArrayList<>();
              byteCount = 0;
            }
          }
        }

        // Commit the created/modified units to offers and create actions
        if (!storageObjects.isEmpty()) {
          storageDao
              .putStorageObjects(tenant, offers, storageObjects)
              .forEach(e -> operation.addAction(StorageAction.create(UPDATE, e)));
        }

        // Index Archive units in Search Engine (properties are already built in each archive)
        if (isFirst && !listIterator.hasNext()) {
          searchService.bulkDeleteRefresh(
              indexedArchiveUnits.stream().map(ArchiveUnit::getId).toList());
        } else {
          searchService.bulkDelete(indexedArchiveUnits.stream().map(ArchiveUnit::getId).toList());
          needRefresh = true;
        }
        isFirst = false;
      }

      if (needRefresh) {
        searchService.refresh();
      }
    }

    // Write delete report to offer
    List<StorageObject> psois =
        List.of(new PathStorageObject(reportPath, operationId, StorageObjectType.rep));
    storageDao
        .putStorageObjects(tenant, offers, psois)
        .forEach(e -> operation.addAction(StorageAction.create(ActionType.CREATE, e)));
  }

  private Map<Long, ArchiveUnit> getStoredUnits(
      Long opId,
      Long tenant,
      List<String> offers,
      List<ArchiveUnit> archiveUnits,
      StorageDao storageDao)
      throws IOException {

    try {
      List<ArchiveUnit> units = storageDao.getArchiveUnits(tenant, offers, opId);
      Map<Long, ArchiveUnit> storedUnits = UnitUtils.mapById(units);
      // Loop on eliminated archive units & remove the stored archive
      archiveUnits.stream().map(ArchiveUnit::getId).forEach(storedUnits::remove);
      return storedUnits;
    } catch (NotFoundException ex) {
      return new HashMap<>();
    }
  }

  // TODO: delete objects after a while via a batch
  // Note. the corresponding atr object must not be deleted.
  private void deleteObjects(OperationDb operation, List<String> offers, Long tenant, Long opId)
      throws IOException {
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.uni);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.uni, MD5, ZERO_BYTES));
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.bin);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.bin, MD5, ZERO_BYTES));
    storageService.deleteObjectIfExists(offers, tenant, opId, StorageObjectType.mft);
    operation.addAction(StorageAction.create(DELETE, opId, StorageObjectType.mft, MD5, ZERO_BYTES));
  }

  protected abstract void checkUnit(ArchiveUnit unit);

  protected abstract ArchiveReporter createReporter(Path reportPath, OperationDb operation)
      throws IOException;
}
