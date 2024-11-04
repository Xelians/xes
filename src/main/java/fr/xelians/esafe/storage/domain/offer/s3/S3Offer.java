/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import fr.xelians.esafe.common.exception.NoSuchObjectException;
import fr.xelians.esafe.common.exception.NotInActiveTierException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.*;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.storage.domain.StorageInputStream;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.ByteStorageObject;
import fr.xelians.esafe.storage.domain.object.PathStorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObject;
import fr.xelians.esafe.storage.domain.object.StorageObjectId;
import fr.xelians.esafe.storage.domain.offer.AbstractStorageOffer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

// Async S3 client : https://www.baeldung.com/java-aws-s3-reactive
// https://stackoverflow.com/questions/59245962/min-io-with-aws-sdk-2-for-java-refuses-to-work-properly
// utiliser v2 & transfert Manager
// https://aws.amazon.com/fr/blogs/developer/introducing-amazon-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
// https://stackoverflow.com/questions/21402735/streaming-files-from-amazon-s3
// https://alexwlchan.net/2019/09/streaming-large-s3-objects/
// https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/transfer/TransferManager.html

// Delete all buckets
// mc --help
// mc alias set myminio http://192.168.1.82:9000 hvA0vo3Nu8yruvQ3 VYo8QiN40bd4PKyC
// mc  rb --force --dangerous myminio

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@ToString
public class S3Offer extends AbstractStorageOffer {

  private static final int BUFFER_SIZE = 16384;
  private static final String SLASH = "/";

  private final Map<Long, String> buckets = new ConcurrentHashMap<>();
  private final Object lock = new Object();

  private final String owner;
  private final Region region;
  private final S3Client s3Client;
  private final S3AsyncClient s3AsyncClient;
  private final int concurrency;
  private final S3Provider provider;
  private final String primaryClass;
  private final String secondaryClass;
  private final boolean needRestore;
  private final long secondarySize;

  public S3Offer(S3Storage s3Storage) {
    super(s3Storage.getName(), s3Storage.isActive(), s3Storage.getStorageTye());

    owner = s3Storage.getOwner();
    region = s3Storage.getRegion();
    concurrency = Math.max(1, s3Storage.getConcurrency());
    provider = s3Storage.getProvider();

    StorageClassDetails scv = provider.getS3StorageClassDetails(s3Storage.getStorageClass());
    primaryClass = scv.primaryStorageClass();
    secondaryClass = scv.secondaryStorageClass();
    needRestore = scv.needRestore();
    secondarySize = scv.secondarySize();

    s3Client = s3Storage.createS3Client();
    s3AsyncClient = s3Storage.createS3AsyncClient();
  }

  @Override
  public List<byte[]> getStorageObjectBytes(Long tenant, List<StorageObjectId> storageObjectIds)
      throws IOException {

    String bucket = getBucket(tenant);
    List<byte[]> bytesList = new ArrayList<>(storageObjectIds.size());
    List<GetObjectFuture> futures = new ArrayList<>(storageObjectIds.size());
    for (StorageObjectId obj : storageObjectIds) {
      String key = getKey(obj.getId(), obj.getType());
      var response =
          s3AsyncClient.getObject(
              b -> b.bucket(bucket).key(key), AsyncResponseTransformer.toBytes());
      futures.add(new GetObjectFuture(key, response));
    }

    waitFuturesBytes(futures, bytesList);
    return bytesList;
  }

  private void waitFuturesBytes(List<GetObjectFuture> futures, List<byte[]> bytesList)
      throws IOException {
    for (var future : futures) {
      try {
        ResponseBytes<GetObjectResponse> response = future.future().get();
        bytesList.add(response.asByteArrayUnsafe());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      } catch (ExecutionException e) {
        if (e.getCause() instanceof NoSuchKeyException) {
          throw new NoSuchObjectException(future.key(), e);
        }
        if (e.getCause() instanceof IOException ioe) {
          throw ioe;
        }
        throw new IOException(e.getCause());
      }
    }
  }

  @Override
  public byte[] getStorageObjectByte(Long tenant, Long id, StorageObjectType type)
      throws IOException {

    String bucket = getBucket(tenant);
    String key = getKey(id, type);

    try {
      ResponseBytes<GetObjectResponse> response =
          s3Client.getObjectAsBytes(b -> b.bucket(bucket).key(key));
      return response.asByteArrayUnsafe();

    } catch (ObjectNotInActiveTierErrorException e) {
      restoreObject(bucket, key);
      throw new NotInActiveTierException(e);
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      if (needRestore) restoreObject(bucket, key);
      throw new IOException(e);
    }
  }

  @Override
  public StorageInputStream getStorageObjectStream(Long tenant, Long id, StorageObjectType type)
      throws IOException {

    String bucket = getBucket(tenant);
    String key = getKey(id, type);

    try {
      ResponseInputStream<GetObjectResponse> inputStream =
          s3Client.getObject(b -> b.bucket(bucket).key(key));
      return new StorageInputStream(
          inputStream.response().contentLength(),
          new BufferedInputStream(inputStream, BUFFER_SIZE));

    } catch (ObjectNotInActiveTierErrorException e) {
      restoreObject(bucket, key);
      throw new NotInActiveTierException(e);
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      if (needRestore) restoreObject(bucket, key);
      throw new IOException(e);
    }
  }

  @Override
  public StorageInputStream getStorageObjectStream(
      Long tenant, Long id, long start, long end, StorageObjectType type) throws IOException {

    String bucket = getBucket(tenant);
    String key = getKey(id, type);
    String range = "bytes=" + start + "-" + end;

    try {
      ResponseInputStream<GetObjectResponse> inputStream =
          s3Client.getObject(b -> b.bucket(bucket).key(key).range(range));
      return new StorageInputStream(
          inputStream.response().contentLength(),
          new BufferedInputStream(inputStream, BUFFER_SIZE));

    } catch (ObjectNotInActiveTierErrorException e) {
      restoreObject(bucket, key);
      throw new NotInActiveTierException(e);
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      if (needRestore) restoreObject(bucket, key);
      throw new IOException(e);
    }
  }

  private void restoreObject(String bucket, String key) throws IOException {
    try {
      RestoreObjectRequest request =
          RestoreObjectRequest.builder()
              .expectedBucketOwner(owner)
              .bucket(bucket)
              .key(key)
              .restoreRequest(b -> b.days(8).glacierJobParameters(c -> c.tier(Tier.STANDARD)))
              .build();
      s3Client.restoreObject(request);
    } catch (ObjectAlreadyInActiveTierErrorException e) {
      log.warn("Failed to restore object", e);
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(key, e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Long> findStorageObjectIdsByType(Long tenant, StorageObjectType type)
      throws IOException {

    if (type == StorageObjectType.lbk || type == StorageObjectType.ope) {

      String bucket = getBucket(tenant);
      String prefix = type.name() + SLASH;
      String ext = "." + type.name();

      try {
        ListObjectsResponse response =
            s3Client.listObjects(b -> b.bucket(bucket).prefix(prefix).delimiter(SLASH));

        return response.contents().stream()
            .map(S3Object::key)
            .filter(s -> s.endsWith(ext))
            .map(s -> s.substring(prefix.length(), s.length() - ext.length()))
            .map(Long::parseLong)
            .toList();

      } catch (S3Exception e) {
        throw new IOException(e);
      }
    }
    throw new IOException(String.format("Method not implemented for object type '%s'", type));
  }

  @Override
  public void putStorageObjects(Long tenant, List<StorageObject> storageObjects)
      throws IOException {

    if (storageObjects.size() == 1) {
      putStorageObject(tenant, storageObjects.getFirst());
    } else {
      String bucket = getBucket(tenant);
      List<Future<PutObjectResponse>> futures =
          storageObjects.stream().map(pso -> putAsyncStorageObject(pso, bucket)).toList();
      try {
        ThreadUtils.joinFutures(futures);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException(e);
      } catch (ExecutionException e) {
        if (e.getCause() instanceof IOException ioe) {
          throw ioe;
        }
        throw new IOException(e.getCause());
      }
    }
  }

  private void putStorageObject(Long tenant, StorageObject storageObject) throws IOException {
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

  // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
  private Future<PutObjectResponse> putAsyncStorageObject(
      StorageObject storageObject, String bucket) {

    String key = getKey(storageObject.getId(), storageObject.getType());
    String storageClass = getStorageClass(storageObject.getSize(), storageObject.getType());

    AsyncRequestBody body = getAsyncrequestBody(storageObject);
    return s3AsyncClient.putObject(b -> b.bucket(bucket).key(key).storageClass(storageClass), body);
  }

  private static AsyncRequestBody getAsyncrequestBody(StorageObjectId storageObject) {
    if (storageObject instanceof PathStorageObject pso)
      return AsyncRequestBody.fromFile(pso.getPath());

    if (storageObject instanceof ByteStorageObject bso)
      return AsyncRequestBody.fromBytes(bso.getBytes());

    throw new InternalException(
        "Failed to put storage objects",
        String.format("Not supported StorageObjectId '%s'", storageObject.getId()));
  }

  @Override
  public void putStorageObject(Path srcPath, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/

    String bucket = getBucket(tenant);
    String key = getKey(id, type);
    String storageClass = getStorageClass(Files.size(srcPath), type);

    try {
      s3Client.putObject(b -> b.bucket(bucket).key(key).storageClass(storageClass), srcPath);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void putStorageObject(byte[] bytes, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/

    String bucket = getBucket(tenant);
    String key = getKey(id, type);
    String storageClass = getStorageClass(bytes.length, type);

    try {
      RequestBody body = RequestBody.fromBytes(bytes);
      s3Client.putObject(b -> b.bucket(bucket).key(key).storageClass(storageClass), body);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void putStorageObject(
      InputStream inputStream, Long tenant, Long id, StorageObjectType type) throws IOException {
    if (inputStream instanceof StorageInputStream storageStream) {

      String bucket = getBucket(tenant);
      String key = getKey(id, type);
      String storageClass = getStorageClass(storageStream.getLength(), type);

      try {
        RequestBody body = RequestBody.fromInputStream(inputStream, storageStream.getLength());
        s3Client.putObject(b -> b.bucket(bucket).key(key).storageClass(storageClass), body);
      } catch (S3Exception e) {
        throw new IOException(e);
      }
    } else {
      Path path = null;
      try {
        path = NioUtils.createTempFile();
        Files.copy(inputStream, path);
        putStorageObject(path, tenant, id, type);
      } finally {
        NioUtils.deletePathQuietly(path);
      }
    }
  }

  @Override
  public void deleteStorageObject(Long tenant, Long id, StorageObjectType type) throws IOException {
    String bucket = getBucket(tenant);
    String key = getKey(id, type);

    try {
      s3Client.deleteObject(b -> b.bucket(bucket).key(key));
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteStorageObjectIfExists(Long tenant, Long id, StorageObjectType type)
      throws IOException {

    String bucket = getBucket(tenant);
    String key = getKey(id, type);

    try {
      s3Client.deleteObject(b -> b.bucket(bucket).key(key));
    } catch (NoSuchKeyException e) {
      log.warn("Failed to delete object - bucket: %s - key: %s".formatted(bucket, key), e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean existsStorageObject(Long tenant, Long id, StorageObjectType type)
      throws IOException {

    String bucket = getBucket(tenant);
    String key = getKey(id, type);

    try {
      s3Client.headObject(b -> b.bucket(bucket).key(key));
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      throw new IOException(e);
    }
    return true;
  }

  @Override
  public byte[] writeLbk(Long tenant, List<OperationDb> operations, long secureNumber, Hash hash)
      throws IOException {

    MessageDigest md = HashUtils.getMessageDigest(hash);
    ByteArrayInOutStream byteArray = new ByteArrayInOutStream();

    try (DigestOutputStream digestOutputStream = new DigestOutputStream(byteArray, md);
        OutputStreamWriter logWriter =
            new OutputStreamWriter(digestOutputStream, StandardCharsets.UTF_8)) {
      writeLbkOps(logWriter, operations);
    }

    String bucket = getBucket(tenant);
    String key = getKey(secureNumber, StorageObjectType.lbk);
    String storageClass = getStorageClass(byteArray.size(), StorageObjectType.lbk);

    try {
      RequestBody body = RequestBody.fromInputStream(byteArray.getInputStream(), byteArray.size());
      s3Client.putObject(b -> b.bucket(bucket).key(key).storageClass(storageClass), body);
    } catch (S3Exception e) {
      throw new IOException(e);
    }

    return md.digest();
  }

  @Override
  public void close() {
    if (s3Client != null) {
      s3Client.close();
    }
    if (s3AsyncClient != null) {
      s3AsyncClient.close();
    }
  }

  private String getKey(long id, StorageObjectType type) {
    return type.name() + SLASH + id + "." + type.name();
  }

  private void deleteBucket(Long tenant) throws IOException {
    try {
      String bucket = getBucket(tenant);
      ListObjectsResponse response =
          s3Client.listObjects(ListObjectsRequest.builder().bucket(bucket).build());
      response.contents().stream()
          .map(S3Object::key)
          .forEach(
              key ->
                  s3Client.deleteObject(
                      DeleteObjectRequest.builder().bucket(bucket).key(key).build()));

      s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
      s3Client
          .waiter()
          .waitUntilBucketNotExists(HeadBucketRequest.builder().bucket(bucket).build());

    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  private String getBucket(Long tenant) throws IOException {
    // Do not replace the following code by computeIfAbsent method
    String bucket = buckets.get(tenant);
    if (bucket == null) {
      synchronized (lock) {
        // We could have ignored the next line (see. java memory barrier) because the bucket
        // creation is idempotent
        bucket = buckets.get(tenant);
        if (bucket == null) {
          bucket = String.format("%s-tenant-%d", this.name, tenant);
          createBucket(bucket);
          buckets.put(tenant, bucket);
        }
      }
    }
    return bucket;
  }

  private void createBucket(String bucket) throws IOException {
    try {
      s3Client.createBucket(
          b -> b.bucket(bucket).createBucketConfiguration(c -> c.locationConstraint(region.id())));
      s3Client.waiter().waitUntilBucketExists(h -> h.bucket(bucket));
    } catch (BucketAlreadyOwnedByYouException exception) {
      log.info("Bucket {} already created", bucket);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  private String getStorageClass(long size, StorageObjectType type) {
    return (size > secondarySize && type == StorageObjectType.bin) ? secondaryClass : primaryClass;
  }

  private record GetObjectFuture(
      String key, CompletableFuture<ResponseBytes<GetObjectResponse>> future) {}
}
