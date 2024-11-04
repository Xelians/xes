/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.dao;

import fr.xelians.esafe.common.exception.NoSuchObjectException;
import fr.xelians.esafe.common.exception.functional.NotFoundException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.ChecksumStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.domain.pack.FilePacker;
import fr.xelians.esafe.storage.domain.pack.Packer;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
public class DirectStorageDao extends AbstractStorageDao {

  public static final String NO_STORAGE_OFFER_DEFINED = "No storage offer defined for tenant: '%s'";
  public static final String NOT_FOUND = "%s not found - tenant: '%s'- id: '%s' - type: '%s'";
  public static final String DETAIL = " - offer: '{}' - tenant: '{}' - id: '{}' - type: '{}'";
  public static final String STREAM_FAILED = "Get object stream failed";
  public static final String STREAM_MSG = STREAM_FAILED + DETAIL;
  public static final String BYTE_FAILED = "Get object byte failed";
  public static final String BYTE_MSG = BYTE_FAILED + DETAIL;
  public static final String BYTES_FAILED = "Get object bytes failed";

  public DirectStorageDao(StorageService storageService) {
    super(storageService);
  }

  public Packer createPacker(Path path) {
    return new FilePacker(path);
  }

  @Override
  public InputStream getObjectStream(
      Long tenant, List<String> offers, Long id, StorageObjectType type) throws IOException {
    IOException ioException = null;

    for (String offer : offers) {
      try {
        return getStorageOffer(offer).getStorageObjectStream(tenant, id, type);
      } catch (IOException ex) {
        log.warn(STREAM_MSG, offer, tenant, id, type);
        ioException = ex;
      }
    }
    if (ioException == null) {
      throw new InternalException(STREAM_FAILED, String.format(NO_STORAGE_OFFER_DEFINED, tenant));
    }

    if (ioException instanceof NoSuchFileException
        || ioException instanceof FileNotFoundException) {
      throw new NotFoundException(
          STREAM_FAILED,
          String.format(NOT_FOUND, StringUtils.capitalize(type.getDesc()), tenant, id, type));
    }
    throw ioException;
  }

  @Override
  public InputStream getObjectStream(
      Long tenant, List<String> offers, Long id, long start, long end, StorageObjectType type)
      throws IOException {
    IOException ioException = null;

    for (String offer : offers) {
      try {
        return getStorageOffer(offer).getStorageObjectStream(tenant, id, start, end, type);
      } catch (IOException ex) {
        log.warn(STREAM_MSG, offer, tenant, id, type);
        ioException = ex;
      }
    }
    if (ioException == null) {
      throw new InternalException(STREAM_FAILED, String.format(NO_STORAGE_OFFER_DEFINED, tenant));
    }

    if (ioException instanceof NoSuchFileException
        || ioException instanceof FileNotFoundException) {
      throw new NotFoundException(
          STREAM_FAILED,
          String.format(NOT_FOUND, StringUtils.capitalize(type.getDesc()), tenant, id, type));
    }
    throw ioException;
  }

  @Override
  public byte[] getObjectByte(Long tenant, List<String> offers, Long id, StorageObjectType type)
      throws IOException {
    IOException ioException = null;

    for (String offer : offers) {
      try {
        return getStorageOffer(offer).getStorageObjectByte(tenant, id, type);
      } catch (IOException ex) {
        log.warn(BYTE_MSG, offer, tenant, id, type);
        ioException = ex;
      }
    }
    if (ioException == null) {
      throw new InternalException(BYTE_FAILED, String.format(NO_STORAGE_OFFER_DEFINED, tenant));
    }

    if (ioException instanceof NoSuchFileException
        || ioException instanceof FileNotFoundException) {
      throw new NotFoundException(
          BYTE_FAILED,
          String.format(NOT_FOUND, StringUtils.capitalize(type.getDesc()), tenant, id, type));
    }
    throw ioException;
  }

  // Use with great care: limit the list size to avoid OOM.
  @Override
  public List<byte[]> getObjectBytes(
      Long tenant, List<String> offers, List<StorageObjectId> storageObjectIds) throws IOException {
    IOException ioException = null;

    for (String offer : offers) {
      try {
        return getStorageOffer(offer).getStorageObjectBytes(tenant, storageObjectIds);
      } catch (IOException ex) {
        log.warn(
            String.format("Get object bytes failed - offer: '%s' - %s", offer, ex.getMessage()));
        ioException = ex;
      }
    }

    if (ioException == null) {
      throw new InternalException(BYTES_FAILED, String.format(NO_STORAGE_OFFER_DEFINED, tenant));
    }

    if (ioException instanceof NoSuchFileException
        || ioException instanceof FileNotFoundException
        || ioException instanceof NoSuchObjectException) {
      throw new NotFoundException(
          BYTES_FAILED,
          String.format(
              "Object not found - tenant:  '%s' - '%s' ", tenant, ioException.getMessage()));
    }

    throw ioException;
  }

  @Override
  public List<ChecksumStorageObject> putStorageObjects(
      Long tenant, List<String> offers, List<StorageObject> storageObjects) throws IOException {

    List<ChecksumStorageObject> csois = new ArrayList<>();
    for (StorageObject storageObject : storageObjects) {
      if (!storageObject.isIgnoreChecksum()) {
        csois.add(createChecksumStorageObjectId(storageObject));
      }
    }

    for (String offer : offers) {
      getStorageOffer(offer).putStorageObjects(tenant, storageObjects);
    }
    return csois;
  }

  @Override
  public void close() {
    // Do nothing
  }
}
