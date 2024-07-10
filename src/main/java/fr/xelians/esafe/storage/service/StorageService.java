/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.service;

import static fr.xelians.esafe.storage.domain.StorageObjectType.lbk;
import static java.util.stream.Collectors.toMap;

import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.StorageProperties;
import fr.xelians.esafe.storage.domain.dao.DirectStorageDao;
import fr.xelians.esafe.storage.domain.dao.EncryptStorageDao;
import fr.xelians.esafe.storage.domain.dao.StorageDao;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.domain.offer.StorageOffer;
import fr.xelians.esafe.storage.domain.offer.fs.FileSystemOffer;
import fr.xelians.esafe.storage.domain.offer.fs.FileSystemStorage;
import fr.xelians.esafe.storage.domain.offer.s3.S3Offer;
import fr.xelians.esafe.storage.domain.offer.s3.S3Storage;
import fr.xelians.esafe.storage.entity.StorageDb;
import fr.xelians.esafe.storage.repository.StorageRepository;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Slf4j
@Service
public class StorageService {

  public static final String FS = "FS:";
  public static final String S3 = "S3:";

  private final Map<String, FileSystemStorage> fsStorages;
  private final Map<String, S3Storage> s3Storages;
  private final Map<String, StorageOffer> storageOffers = new HashMap<>();

  private final StorageRepository storageRepository;
  private final SecretKeyService secretKeyService;

  public StorageService(
      StorageRepository storageRepository,
      StorageProperties storageProperties,
      SecretKeyService secretKeyService) {

    this.storageRepository = storageRepository;
    this.secretKeyService = secretKeyService;

    fsStorages =
        storageProperties.getFs().stream()
            .collect(toMap(FileSystemStorage::getName, Function.identity()));
    fsStorages
        .values()
        .forEach(fs -> storageOffers.put(FS + fs.getName(), new FileSystemOffer(fs)));

    s3Storages =
        storageProperties.getS3().stream().collect(toMap(S3Storage::getName, Function.identity()));
    s3Storages.values().forEach(s3 -> storageOffers.put(S3 + s3.getName(), new S3Offer(s3)));
  }

  @PreDestroy
  public void close() {
    storageOffers.values().forEach(StorageOffer::close);
  }

  public long getSecureNumber(Long tenant) {
    return getStorageLog(tenant).getSecureNumber();
  }

  public List<StorageOffer> getStorageOffers(List<String> offers) {
    return offers.stream().map(storageOffers::get).toList();
  }

  public FileSystemStorage getFsStorage(String name) {
    Assert.hasText(name, "File System storage identifier cannot be null or empty");

    FileSystemStorage fs = fsStorages.get(name);
    if (fs == null) {
      throw new NotFoundException(
          "FS Storage not found",
          String.format("File System storage with id '%s' not found", name));
    }
    return fs;
  }

  public S3Storage getS3Storage(String id) {
    Assert.hasText(id, "S3 storage identifier cannot be null or empty");

    S3Storage s3 = s3Storages.get(id);
    if (s3 == null) {
      throw new NotFoundException(
          "S3 Storage not found", String.format("S3 storage with id '%s' not found", id));
    }
    return s3;
  }

  public StorageOffer getStorageOffer(String offer) {
    StorageOffer storageOffer = storageOffers.get(offer);
    if (storageOffer == null) {
      throw new InternalException(
          "Get storage offer failed", String.format("Storage Offer not found '%s'", offer));
    }
    return storageOffer;
  }

  public boolean existsStorageOffer(String offer) {
    if (offer.startsWith(FS)) {
      String name = offer.substring(FS.length());
      return fsStorages.get(name) != null;
    }
    if (offer.startsWith(S3)) {
      String name = offer.substring(S3.length());
      return s3Storages.get(name) != null;
    }
    return false;
  }

  public void deleteObjectsQuietly(
      List<String> offers, Long tenant, List<? extends StorageObjectId> getObjects) {
    for (String offer : offers) {
      StorageOffer storageOffer = getStorageOffer(offer);
      for (StorageObjectId logStorageObjectId : getObjects) {
        deleteObjectQuietly(
            storageOffer, tenant, logStorageObjectId.getId(), logStorageObjectId.getType());
      }
    }
  }

  public void deleteObjectQuietly(
      List<String> offers, Long tenant, Long id, StorageObjectType type) {
    for (String offer : offers) {
      deleteObjectQuietly(getStorageOffer(offer), tenant, id, type);
    }
  }

  private void deleteObjectQuietly(
      StorageOffer storageOffer, Long tenant, Long id, StorageObjectType type) {
    try {
      storageOffer.deleteStorageObjectIfExists(tenant, id, type);
    } catch (IOException ex) {
      log.warn("Rollback exception: {}", ex.getMessage());
    }
  }

  public void deleteObject(List<String> offers, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    for (String offer : offers) {
      getStorageOffer(offer).deleteStorageObject(tenant, id, type);
    }
  }

  public void deleteObjectIfExists(
      List<String> offers, Long tenant, Long id, StorageObjectType type) throws IOException {
    for (String offer : offers) {
      getStorageOffer(offer).deleteStorageObjectIfExists(tenant, id, type);
    }
  }

  public InputStream getLogbookStream(TenantDb tenantDb, Long id) throws IOException {
    // Logbook is not encrypted
    try (StorageDao dao = new DirectStorageDao(this)) {
      return dao.getObjectStream(tenantDb.getId(), tenantDb.getStorageOffers(), id, lbk);
    }
  }

  public void copy(
      Long tenant, List<String> srcOffers, Long id, StorageObjectType type, String dstOffer)
      throws IOException {
    try (StorageDao dao = new DirectStorageDao(this);
        InputStream is = dao.getObjectStream(tenant, srcOffers, id, type)) {
      getStorageOffer(dstOffer).putStorageObject(is, tenant, id, type);
    }
  }

  public byte[] getChecksum(
      Long tenant, List<String> srcOffers, Long id, StorageObjectType type, Hash hash)
      throws IOException {
    try (StorageDao dao = new DirectStorageDao(this);
        InputStream is = dao.getObjectStream(tenant, srcOffers, id, type)) {
      return HashUtils.checksum(hash, is);
    }
  }

  public List<Long> findLbkIds(TenantDb tenantDb, List<String> srcOffers) throws IOException {
    return findObjectIdsByType(tenantDb.getId(), srcOffers, lbk);
  }

  private List<Long> findObjectIdsByType(
      Long tenant, List<String> srcOffers, StorageObjectType type) throws IOException {
    IOException ioException = null;

    for (String offer : srcOffers) {
      try {
        return getStorageOffer(offer).findStorageObjectIdsByType(tenant, type);
      } catch (IOException ex) {
        log.warn(
            "Failed to find object by type in storageOffer: {} ", getStorageOffer(offer).getName());
        ioException = ex;
      }
    }
    if (ioException == null) {
      throw new InternalException(
          "Find objects by type failed",
          String.format("No Storage Offer defined for tenant: '%s'", tenant));
    }
    throw ioException;
  }

  public StorageDb getStorageLog(Long tenant) {
    return storageRepository
        .findById(tenant)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Storage log not found", String.format("Tenant with id %s not found", tenant)));
  }

  public StorageDao createStorageDao(TenantDb tenantDb) {
    return BooleanUtils.isTrue(tenantDb.getEncrypted())
        ? new EncryptStorageDao(this, secretKeyService.getSecretKey(tenantDb.getId()))
        : new DirectStorageDao(this);
  }
}

//    public void asyncWrite(HttpContext httpContext, Path srcPath, ObjectType type, long id) throws
// IOException {
//
//        Executor e = Executors.newFixedThreadPool(3);
//        CompletionService<Void> ecs = new ExecutorCompletionService<>(e);
//
//        List<StorageOffer> storageOffers = strategies.get(httpContext.getTenant());
//        int n = storageOffers.size();
//        List<Future<Void>> futures = new ArrayList<>(n);
//
//        try {
//            for (StorageOffer s : storageOffers) {
//                Callable<Void> task = () -> {
//                    s.putObject(srcPath, httpContext.getTenant(), type, id);
//                    return null;
//                };
//                futures.add(ecs.submit(task));
//            }
//
//            for (int i = 0; i < n; ++i) {
//                try {
//                    ecs.take().get();
//                } catch (InterruptedException | ExecutionException ex) {
//                    throw new IOException(ex.getCause());
//                }
//            }
//        } finally {
//            for (Future<Void> f : futures) {
//                f.cancel(true);
//                // Check for Interruption or by polling Thread.currentThread().isInterrupted() ;
//            }
//        }
//    }
