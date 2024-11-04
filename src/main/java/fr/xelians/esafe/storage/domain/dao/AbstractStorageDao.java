/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.dao;

import fr.xelians.esafe.archive.domain.unit.ArchiveUnit;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.referential.entity.AgencyDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.*;
import fr.xelians.esafe.storage.domain.offer.StorageOffer;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/*
 * @author Emmanuel Deviller
 */
public abstract class AbstractStorageDao implements StorageDao {

  public static final Hash HASH = Hash.MD5;

  protected StorageService storageService;

  protected AbstractStorageDao(StorageService storageService) {
    this.storageService = storageService;
  }

  protected StorageOffer getStorageOffer(String offer) {
    return storageService.getStorageOffer(offer);
  }

  @Override
  public List<AgencyDb> getAgencies(Long tenant, List<String> offers) throws IOException {
    try {
      byte[] bytes = getObjectByte(tenant, offers, 1L, StorageObjectType.age);
      return JsonService.toAgencies(bytes);
    } catch (NotFoundException ex) {
      return new ArrayList<>();
    }
  }

  @Override
  public List<ArchiveUnit> getArchiveUnits(Long tenant, List<String> offers, Long operationId)
      throws IOException {
    byte[] bytes = getObjectByte(tenant, offers, operationId, StorageObjectType.uni);
    return JsonService.toArchiveUnits(bytes);
  }

  @Override
  public List<ArchiveUnit> getArchiveUnits(
      Long tenant, List<String> offers, List<Long> operationIds) throws IOException {
    List<StorageObjectId> storageObjectIds =
        operationIds.stream().map(id -> new StorageObjectId(id, StorageObjectType.uni)).toList();
    List<ArchiveUnit> archiveUnits = new ArrayList<>();
    for (byte[] bytes : getObjectBytes(tenant, offers, storageObjectIds)) {
      archiveUnits.addAll(JsonService.toArchiveUnits(bytes));
    }
    return archiveUnits;
  }

  @Override
  public InputStream getManifestStream(Long tenant, List<String> offers, Long id)
      throws IOException {
    return getObjectStream(tenant, offers, id, StorageObjectType.mft);
  }

  @Override
  public InputStream getAtrStream(Long tenant, List<String> offers, Long id) throws IOException {
    return getObjectStream(tenant, offers, id, StorageObjectType.atr);
  }

  @Override
  public InputStream getReportStream(Long tenant, List<String> offers, Long id) throws IOException {
    return getObjectStream(tenant, offers, id, StorageObjectType.rep);
  }

  @Override
  public InputStream getDipStream(Long tenant, List<String> offers, Long id) throws IOException {
    return getObjectStream(tenant, offers, id, StorageObjectType.dip);
  }

  @Override
  public InputStream getBinaryObjectStream(
      Long tenant, List<String> offers, Long operationId, long[] pos, Long id) throws IOException {
    if (pos != null && pos[0] >= 0 && pos[1] >= pos[0]) {
      return getObjectStream(tenant, offers, operationId, pos[0], pos[1], StorageObjectType.bin);
    }
    throw new NotFoundException(
        "Binary object not found on storage",
        String.format("Object not found: %s - %s - %s ", tenant, StorageObjectType.bin, id));
  }

  @Override
  public InputStream getAusStream(Long tenant, List<String> offers, Long id) throws IOException {
    return getObjectStream(tenant, offers, id, StorageObjectType.aus);
  }

  protected ChecksumStorageObject createChecksumStorageObjectId(StorageObject storageObject)
      throws IOException {
    if (storageObject instanceof PathStorageObject psoi)
      return new ChecksumStorageObject(
          psoi.getId(), psoi.getType(), HASH, HashUtils.checksum(HASH, psoi.getPath()));

    if (storageObject instanceof ByteStorageObject bsoi)
      return new ChecksumStorageObject(
          bsoi.getId(), bsoi.getType(), HASH, HashUtils.checksum(HASH, bsoi.getBytes()));

    throw new InternalException("Failed to create checksum storage object Id");
  }
}
