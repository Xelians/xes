/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.admin.domain.scanner;

import com.machinezoo.noexception.Exceptions;
import fr.xelians.esafe.common.constant.Logbook;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.operation.domain.OperationType;
import fr.xelians.esafe.operation.domain.StorageAction;
import fr.xelians.esafe.operation.entity.OperationSe;
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

public abstract class LbkIterator implements AutoCloseable, Iterator<OperationSe> {

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

  public OperationSe next() {
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

  private OperationSe nextOperation() throws IOException {
    OperationSe operationSe = null;

    oups:
    while (line != null) {
      String[] tokens = parseLine(line);

      switch (tokens[0]) {
        case OPERATION_BEGIN -> {
          assertNull(operationSe);
          // TOD0 should be abstract to be able to filter
          operationSe = createOperation(tokens);
        }
        case ACTION_CREATE -> {
          assertNotNull(operationSe);
          actionCreate(operationSe, tokens);
        }
        case ACTION_UPDATE -> {
          assertNotNull(operationSe);
          actionUpdate(operationSe, tokens);
        }
        case ACTION_DELETE -> {
          assertNotNull(operationSe);
          actionDelete(operationSe, tokens);
        }
        case OPERATION_COMMIT -> {
          assertNotNull(operationSe);
          commitOperation(operationSe, tokens);
          return operationSe;
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

  protected abstract void actionCreate(OperationSe operationSe, String[] tokens);

  protected abstract void actionUpdate(OperationSe operationSe, String[] tokens);

  protected abstract void actionDelete(OperationSe operationSe, String[] tokens);

  protected void assertNull(OperationSe operationSe) {
    if (operationSe != null) {
      throw new InternalException(
          "Null operation", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
    }
  }

  protected void assertNotNull(OperationSe operationSe) {
    if (operationSe == null) {
      throw new InternalException(
          "Non null operation", String.format(PARSE_FAILED, tenantDb.getId(), lbkId));
    }
  }

  // BEGIN;CREATE_PROFILE;20;250;4;ESAFE_TEST;2023-12-02T22:22:51.738909;CREATE_PROFILE;outcome;Backup done;obid;obinfo;obdata
  // CREATE;453;bin;MD5;2e32ee6b3922991e5a65f2d14094a22f
  // CREATE;456;uni;MD5;1b7e9e582dc07b757f10374ec7701116
  // COMMIT;SECURE_OPERATION;109;868;2022-04-25T00:49:43.608007
  protected OperationSe createOperation(String[] tokens) {
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

    return new OperationSe(
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

  protected void commitOperation(OperationSe operationSe, String[] tokens) {
    operationSe.setModified(LocalDateTime.parse(tokens[4]));

    // Check if the checksum of this secure operation exists and is valid
    if (checkStorageChain() && OperationType.SECURING == operationSe.getType()) {
      checkChecksums(operationSe);
    }
  }

  protected void checkChecksums(OperationSe operationSe) {
    for (StorageAction storageAction : operationSe.getStorageActions()) {
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
