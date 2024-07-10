/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.fs;

import static java.nio.file.StandardOpenOption.*;

import fr.xelians.esafe.common.constant.Env;
import fr.xelians.esafe.common.exception.NoSuchObjectException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Hash;
import fr.xelians.esafe.common.utils.HashUtils;
import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.storage.domain.StorageInputStream;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.domain.offer.AbstractStorageOffer;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.input.BoundedInputStream;

@ToString
public class FileSystemOffer extends AbstractStorageOffer {

  private static final OpenOption[] BUFF_OPT = {CREATE, TRUNCATE_EXISTING, WRITE};
  private static final OpenOption[] SYNC_OPT = {CREATE, TRUNCATE_EXISTING, WRITE, DSYNC};

  private static final String TMP = ".tmp";
  private static final String OPE_DIR = "ope";
  private static final String DAT_DIR = "dat";
  private static final String LBK_DIR = "lbk";
  private static final String REF_DIR = "ref";

  private final Map<Long, OfferPath> offerPaths = new ConcurrentHashMap<>();
  private final Object lock = new Object();
  private final String root;
  private final OpenOption[] openOptions;
  private final StorageCapacity capacity;
  private final PathMaker pathMaker;
  protected final int concurrency;

  public FileSystemOffer(FileSystemStorage fsStorage) {
    super(fsStorage.getName(), fsStorage.isActive(), fsStorage.getStorageTye());

    root = fsStorage.getRoot();
    openOptions = fsStorage.isSync() ? SYNC_OPT : BUFF_OPT;
    concurrency = Math.max(1, fsStorage.getConcurrency());
    capacity = fsStorage.getCapacity();
    pathMaker =
        switch (capacity) {
          case SMALL -> SmallPathMaker.INSTANCE;
          case MEDIUM -> MediumPathMaker.INSTANCE;
          case LARGE -> LargePathMaker.INSTANCE;
        };
  }

  @Override
  public List<byte[]> getStorageObjectBytes(Long tenant, List<StorageObjectId> storageObjectIds)
      throws IOException {
    List<byte[]> bytesList = new ArrayList<>(storageObjectIds.size());

    if (concurrency <= 1) {
      for (StorageObjectId storageObjectId : storageObjectIds) {
        bytesList.add(
            getStorageObjectByte(tenant, storageObjectId.getId(), storageObjectId.getType()));
      }
    } else {
      List<GetFileFuture> futures = new ArrayList<>(concurrency);
      List<AsynchronousFileChannel> channels = new ArrayList<>(concurrency);

      try {
        for (StorageObjectId storageObjectId : storageObjectIds) {
          Path path = getOfferPath(tenant, storageObjectId.getId(), storageObjectId.getType());
          long size = Files.size(path);
          if (size > Integer.MAX_VALUE) {
            throw new OutOfMemoryError(String.format("Required array size too large for %s", path));
          }
          var buffer = ByteBuffer.allocate((int) size);
          var channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
          channels.add(channel);
          Future<Integer> future = channel.read(buffer, 0);
          futures.add(new GetFileFuture(path, buffer, future));
          if (futures.size() >= concurrency) {
            waitFuturesBytes(futures, bytesList);
            NioUtils.closeQuietly(channels);
            futures = new ArrayList<>(concurrency);
            channels = new ArrayList<>(concurrency);
          }
        }
        waitFuturesBytes(futures, bytesList);
      } finally {
        NioUtils.closeQuietly(channels);
      }
    }
    return bytesList;
  }

  protected void waitFuturesBytes(List<GetFileFuture> futures, List<byte[]> bytesList)
      throws IOException {
    for (var future : futures) {
      try {
        future.future().get();
        bytesList.add(future.buffer().array());
        future.buffer().clear();
      } catch (InterruptedException | ExecutionException e) {
        if (e.getCause() instanceof NoSuchFileException) {
          throw new NoSuchObjectException(future.path().toString(), e);
        }
        throw new IOException(e);
      }
    }
  }

  @Override
  public byte[] getStorageObjectByte(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path path = getOfferPath(tenant, id, type);
    return Files.readAllBytes(path);
  }

  @Override
  public StorageInputStream getStorageObjectStream(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path path = getOfferPath(tenant, id, type);
    return new StorageInputStream(
        Files.size(path), new BufferedInputStream(Files.newInputStream(path)));
  }

  // Note. the stream must be closed by the caller
  @Override
  public StorageInputStream getStorageObjectStream(
      Long tenant, Long id, long start, long end, StorageObjectType type) throws IOException {
    if (start > end) {
      throw new InternalException(
          "Get storage binary object failed",
          String.format(
              "Tenant '%s' - Type '%s' - id %s - Range: start %s > end %s",
              tenant, type, id, start, end));
    }

    Path path = getOfferPath(tenant, id, type);

    BoundedInputStream bis =
        BoundedInputStream.builder()
            .setInputStream(new BufferedInputStream(Files.newInputStream(path)))
            .setMaxCount(end + 1)
            .get();
    bis.skipNBytes(start);
    return new StorageInputStream(end + 1 - start, bis);
  }

  @Override
  public List<Long> findStorageObjectIdsByType(Long tenant, StorageObjectType type)
      throws IOException {
    if (type == StorageObjectType.lbk || type == StorageObjectType.ope) {
      Path dirPath = getOfferPath(tenant, type);
      String ext = "." + type.name();
      int size = ext.length();

      try (Stream<Path> stream = Files.list(dirPath)) {
        return stream
            .filter(p -> !Files.isDirectory(p))
            .map(p -> p.getFileName().toString())
            .filter(s -> s.endsWith(ext))
            .map(s -> s.substring(0, s.length() - size))
            .map(Long::parseLong)
            .toList();
      }
    }
    throw new IOException(String.format("Method not implemented for object type '%s'", type));
  }

  @Override
  public void putStorageObjects(Long tenant, List<StorageObject> storageObjects)
      throws IOException {
    for (StorageObjectId storageObject : storageObjects) {
      switch (storageObject) {
        case PathStorageObject pso -> putStorageObject(
            pso.getPath(), tenant, pso.getId(), pso.getType());
        case ByteStorageObject bso -> putStorageObject(
            bso.getBytes(), tenant, bso.getId(), bso.getType());
        default -> throw new InternalException(
            "Failed to put storage objects",
            String.format("Not supported StorageObjectId '%s'", storageObject.getId()));
      }
    }
  }

  //  @Override
  //  public void putByteStorageObjects(Long tenant, List<ByteStorageObjectId> byteStorageObjects)
  //      throws IOException {
  //    for (ByteStorageObjectId byteStorageObject : byteStorageObjects) {
  //      putStorageObject(
  //          byteStorageObject.getBytes(),
  //          tenant,
  //          byteStorageObject.getId(),
  //          byteStorageObject.getType());
  //    }
  //  }

  @Override
  public void putStorageObject(Path srcPath, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path dstPath = getOfferPath(tenant, id, type);
    if (Files.exists(dstPath)) {
      Path tmpPath = dstPath.resolveSibling(dstPath.getFileName() + TMP);
      Files.copy(srcPath, tmpPath, StandardCopyOption.REPLACE_EXISTING);
      Files.move(
          tmpPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } else {
      Files.createDirectories(dstPath.getParent());
      Files.copy(srcPath, dstPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Override
  public void putStorageObject(byte[] bytes, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path dstPath = getOfferPath(tenant, id, type);
    if (Files.exists(dstPath)) {
      Path tmpPath = dstPath.resolveSibling(dstPath.getFileName() + TMP);
      Files.write(tmpPath, bytes);
      Files.move(
          tmpPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } else {
      Files.createDirectories(dstPath.getParent());
      Files.write(dstPath, bytes);
    }
  }

  @Override
  public void putStorageObject(
      InputStream inputStream, Long tenant, Long id, StorageObjectType type) throws IOException {
    Path dstPath = getOfferPath(tenant, id, type);
    if (Files.exists(dstPath)) {
      Path tmpPath = dstPath.resolveSibling(dstPath.getFileName() + TMP);
      Files.copy(inputStream, tmpPath, StandardCopyOption.REPLACE_EXISTING);
      Files.move(
          tmpPath, dstPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } else {
      Files.createDirectories(dstPath.getParent());
      Files.copy(inputStream, dstPath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  @Override
  public boolean existsStorageObject(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path dstPath = getOfferPath(tenant, id, type);
    return Files.exists(dstPath);
  }

  @Override
  public void deleteStorageObject(Long tenant, Long id, StorageObjectType type) throws IOException {
    Path path = getOfferPath(tenant, id, type);
    Files.delete(path);
  }

  @Override
  public void deleteStorageObjectIfExists(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    Path path = getOfferPath(tenant, id, type);
    Files.deleteIfExists(path);
  }

  // Only the Secure Operation Scheduler can update the log (i.e. 1 thread)
  @Override
  public byte[] writeLbk(Long tenant, List<OperationDb> operations, long secureNumber, Hash hash)
      throws IOException {

    MessageDigest md = HashUtils.getMessageDigest(hash);
    Path secPath = getOfferPath(tenant, secureNumber, StorageObjectType.lbk);
    Path tmpPath = secPath.resolveSibling(secPath.getFileName() + TMP);

    try (OutputStream is = Files.newOutputStream(tmpPath, openOptions);
        DigestOutputStream dis = new DigestOutputStream(is, md);
        Writer writer = new OutputStreamWriter(dis, StandardCharsets.UTF_8);
        BufferedWriter logWriter = new BufferedWriter(writer)) {
      writeLbkOps(logWriter, operations);
    }
    Files.move(
        tmpPath, secPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    return md.digest();
  }

  private Path getOfferPath(Long tenant, StorageObjectType type) throws IOException {
    OfferPath offerPath = getOfferPath(tenant, root);

    if (type == StorageObjectType.ope) {
      return offerPath.ope;
    }

    if (type == StorageObjectType.lbk) {
      return offerPath.log;
    }

    return offerPath.dat;
  }

  protected Path getOfferPath(Long tenant, Long id, StorageObjectType type) throws IOException {
    if (id < 0) {
      throw new InternalException(
          "Get offer path failed",
          String.format("Tenant '%s' - Type '%s' - id '%s' is negative", tenant, type, id));
    }
    OfferPath offerPath = getOfferPath(tenant, root);
    return switch (type) {
      case ope -> offerPath.ope.resolve(id + "." + type);
      case lbk -> offerPath.log.resolve(id + "." + type);
      case age, acc, ing, pro, rul, usr, ten, org, ind, gra -> offerPath.ref.resolve(
          id + "." + type);
      default -> offerPath.dat.resolve(pathMaker.makePath(type, id));
    };
  }

  @Override
  public void close() {
    // Do nothing now
  }

  private OfferPath getOfferPath(Long tenant, String root) throws IOException {
    OfferPath offerPath = offerPaths.get(tenant);
    if (offerPath == null) {
      synchronized (lock) {
        // We could have ignored the next line (see. java memory barrier) because bucket creation is
        // idempotent
        offerPath = offerPaths.get(tenant);
        if (offerPath == null) {
          offerPath = new OfferPath(tenant, root);
          offerPaths.put(tenant, offerPath);
        }
      }
    }
    return offerPath;
  }

  @Getter
  private static class OfferPath {

    private final Path root;
    private final Path ope;
    private final Path log;
    private final Path ref;
    private final Path dat;

    public OfferPath(Long tenant, String root) throws IOException {
      this.root = Env.OFFER_PATH.resolve(root).resolve(String.valueOf(tenant));
      ope = this.root.resolve(OPE_DIR);
      log = this.root.resolve(LBK_DIR);
      ref = this.root.resolve(REF_DIR);
      dat = this.root.resolve(DAT_DIR);

      Files.createDirectories(ope);
      Files.createDirectories(log);
      Files.createDirectories(ref);
      Files.createDirectories(dat);
    }
  }

  public record GetFileFuture(Path path, ByteBuffer buffer, Future<Integer> future) {}
}
