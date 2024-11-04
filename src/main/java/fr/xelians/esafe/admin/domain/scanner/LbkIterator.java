/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.admin.domain.scanner;

import com.machinezoo.noexception.Exceptions;
import fr.xelians.esafe.common.constant.Logbook;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.logbook.domain.model.LogbookOperation;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.organization.entity.TenantDb;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/*
 * @author Emmanuel Deviller
 */
public abstract class LbkIterator implements AutoCloseable, Iterator<LogbookOperation> {

  private static final String PARSE_FAILED =
      "Failed to parse corrupted operation log - tenant: '%s' - id: '%s'";

  public static final String OPERATION_BEGIN = "BEGIN";
  public static final String OPERATION_COMMIT = "COMMIT";
  public static final String ACTION_CREATE = "CREATE";
  public static final String ACTION_DELETE = "DELETE";
  public static final String ACTION_UPDATE = "UPDATE";

  protected final StorageService storageService;
  protected final TenantDb tenantDb;
  protected final List<String> offers;
  protected BufferedReader lbkReader;
  protected String line;
  protected long lbkId = -1;

  private final Map<Long, byte[]> checksums = new HashMap<>();
  private boolean hasNext;
  private boolean done;

  protected LbkIterator(TenantDb tenantDb, List<String> offers, StorageService storageService) {
    this.tenantDb = tenantDb;
    this.offers = offers;
    this.storageService = storageService;
  }

  public boolean hasNext() {
    if (done) {
      return hasNext;
    }
    done = true;
    hasNext = hastNextOperation();
    return hasNext;
  }

  protected abstract boolean hastNextOperation();

  public LogbookOperation next() {
    if (!done && !hasNext()) {
      throw new NoSuchElementException("Operation not found");
    }
    if (line == null) {
      throw new NoSuchElementException("Operation not found");
    }
    done = false;

    try {
      return nextOperation();
    } catch (IOException ex) {
      throw Exceptions.sneak().handle(ex);
    }
  }

  private LogbookOperation nextOperation() throws IOException {
    LogbookOperation logbookOperation = null;

    oups:
    while (line != null) {
      String[] tokens = parseLine(line);

      switch (tokens[0]) {
        case OPERATION_BEGIN -> {
          assertNull(logbookOperation);
          // TOD0 should be abstract to be able to filter
          logbookOperation = createOperation(tokens);
        }
        case ACTION_CREATE -> {
          assertNotNull(logbookOperation);
          actionCreate(logbookOperation, tokens);
        }
        case ACTION_UPDATE -> {
          assertNotNull(logbookOperation);
          actionUpdate(logbookOperation, tokens);
        }
        case ACTION_DELETE -> {
          assertNotNull(logbookOperation);
          actionDelete(logbookOperation, tokens);
        }
        case OPERATION_COMMIT -> {
          assertNotNull(logbookOperation);
          commitOperation(logbookOperation, tokens);
          return logbookOperation;
        }
        default -> {
          break oups;
        }
      }

      // read next line in the operation
      line = lbkReader.readLine();
    }
    throw new InternalException(
        "Read next logbook operation failed", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
  }

  protected abstract void actionCreate(LogbookOperation logbookOperation, String[] tokens);

  protected abstract void actionUpdate(LogbookOperation logbookOperation, String[] tokens);

  protected abstract void actionDelete(LogbookOperation logbookOperation, String[] tokens);

  protected void assertNull(LogbookOperation logbookOperation) {
    if (logbookOperation != null) {
      throw new InternalException(
          "Null operation", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
    }
  }

  protected void assertNotNull(LogbookOperation logbookOperation) {
    if (logbookOperation == null) {
      throw new InternalException(
          "Non null operation", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
    }
  }

  // BEGIN;CREATE_PROFILE;20;250;4;ESAFE_TEST;2023-12-02T22:22:51.738909;CREATE_PROFILE;outcome;Backup done;obid;obinfo;obdata
  // CREATE;453;bin;MD5;2e32ee6b3922991e5a65f2d14094a22f
  // CREATE;456;uni;MD5;1b7e9e582dc07b757f10374ec7701116
  // COMMIT;SECURE_OPERATION;109;868;2022-04-25T00:49:43.608007
  protected LogbookOperation createOperation(String[] tokens) {
    OperationType type = OperationType.valueOf(tokens[1]);
    long te = Long.parseLong(tokens[2]);
    long id = Long.parseLong(tokens[3]);
    String userId = tokens[4];
    String applicationId = tokens[5];
    String typeInfo = tokens[6];
    String outcome = tokens[7];
    String message = tokens[8];
    String obId = tokens[9];
    String obInfo = tokens[10];
    String obData = tokens[11];
    LocalDateTime created = LocalDateTime.parse(tokens[12]);

    return new LogbookOperation(
        id,
        te,
        type,
        userId,
        applicationId,
        created,
        null,
        typeInfo,
        outcome,
        message,
        obId,
        obInfo,
        obData);
  }

  protected void commitOperation(LogbookOperation logbookOperation, String[] tokens) {
    logbookOperation.setModified(LocalDateTime.parse(tokens[4]));

    // Check if the checksum of this secure operation exists and is valid
    if (checkStorageChain() && OperationType.TRACEABILITY == logbookOperation.getType()) {
      checkChecksums(logbookOperation);
    }
  }

  protected void checkChecksums(LogbookOperation logbookOperation) {
    for (StorageAction storageAction : logbookOperation.getStorageActions()) {
      byte[] checksum = checksums.get(storageAction.getId());
      if (checksum == null || !Arrays.equals(checksum, storageAction.getChecksum())) {
        throw new InternalException(
            "Check logbook digest failed",
            String.format(
                "Bad logbook secure chain - tenant: %s - id: %s.log - incoherent checksums - secured: %s - stored: %s",
                tenantDb.getId(),
                storageAction.getId(),
                HashUtils.encodeHex(checksum),
                HashUtils.encodeHex(storageAction.getChecksum())));
      }
    }
  }

  // Line must end with a non-empty char
  private String[] parseLine(String line) {
    String[] tokens = new String[13];
    int i = 0;
    int oldPos = 0;

    while (i < tokens.length && oldPos < line.length()) {
      int newPos = line.indexOf(Logbook.LSEP, oldPos);
      if (newPos >= 0) {
        tokens[i] = line.substring(oldPos, newPos);
        oldPos = newPos + 1;
        i++;
      } else {
        // Last token
        tokens[i] = line.substring(oldPos);
        return tokens;
      }
    }
    throw new InternalException(
        "Parse logbook line failed", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
  }

  protected boolean checkStorageChain() {
    return false;
  }

  protected void addChecksums() throws IOException {
    byte[] checksum = null;

    // Ensure checksums are equals on each offer
    for (String offer : tenantDb.getStorageOffers()) {
      byte[] cs =
          storageService.getChecksum(
              tenantDb.getId(), List.of(offer), lbkId, StorageObjectType.lbk, Hash.SHA256);
      if (checksum == null) {
        checksum = cs;
      } else if (!Arrays.equals(checksum, cs)) {
        throw new InternalException(
            "Check logbook digest failed",
            String.format(
                "Bad logbook secure chain - tenant: %s - id: %s - checksums are different between storage offers- '%s' != '%s' ",
                tenantDb.getId(), lbkId, HashUtils.encodeHex(checksum), HashUtils.encodeHex(cs)));
      }
    }
    checksums.put(lbkId, checksum);
  }

  protected void createLbkReader() throws IOException {
    if (checkStorageChain()) {
      addChecksums();
    }

    InputStream is = storageService.getLogbookStream(tenantDb, lbkId);
    InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
    lbkReader = new BufferedReader(reader);
  }

  protected void closeLbkReader() throws IOException {
    if (lbkReader != null) {
      // This also closes underlying streams
      lbkReader.close();
      lbkReader = null;
    }
  }

  @Override
  public void close() throws IOException {
    closeLbkReader();
  }
}
