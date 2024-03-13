/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.storage.domain.offer.s3;

import fr.xelians.esafe.common.exception.NoSuchObjectException;
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

@Slf4j
@ToString
public class S3Offer extends AbstractStorageOffer {

  private static final int DEFAULT_BUFFER_SIZE = 16384;
  private static final String PATH_SEP = "/";

  private final Map<Long, String> buckets = new ConcurrentHashMap<>();
  private final Object lock = new Object();
  private final Region region;
  private final S3Client s3Client;
  private final S3AsyncClient s3AsyncClient;
  private final int concurrency;

  public S3Offer(S3Storage s3Storage) {
    super(s3Storage.getName(), s3Storage.isActive(), s3Storage.getStorageTye());

    region = s3Storage.getRegion();
    concurrency = Math.max(1, s3Storage.getConcurrency());
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
      GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
      var body = s3AsyncClient.getObject(request, AsyncResponseTransformer.toBytes());
      futures.add(new GetObjectFuture(key, body));
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
    try {
      String bucket = getBucket(tenant);
      String key = getKey(id, type);
      GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
      ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
      return response.asByteArrayUnsafe();
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public StorageInputStream getStorageObjectStream(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    try {
      String bucket = getBucket(tenant);
      String key = getKey(id, type);
      GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();
      ResponseInputStream<GetObjectResponse> inputStream = s3Client.getObject(request);
      return new StorageInputStream(
          inputStream.response().contentLength(),
          new BufferedInputStream(inputStream, DEFAULT_BUFFER_SIZE));
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public StorageInputStream getStorageObjectStream(
      Long tenant, Long id, long start, long end, StorageObjectType type) throws IOException {
    try {
      ResponseInputStream<GetObjectResponse> inputStream =
          s3Client.getObject(
              GetObjectRequest.builder()
                  .bucket(getBucket(tenant))
                  .key(getKey(id, type))
                  .range("bytes=" + start + "-" + end)
                  .build());
      return new StorageInputStream(
          inputStream.response().contentLength(),
          new BufferedInputStream(inputStream, DEFAULT_BUFFER_SIZE));
    } catch (NoSuchKeyException e) {
      throw new NoSuchObjectException(getKey(id, type), e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public List<Long> findStorageObjectIdsByType(Long tenant, StorageObjectType type)
      throws IOException {
    if (type == StorageObjectType.lbk || type == StorageObjectType.ope) {
      try {
        String prefix = type.name() + PATH_SEP;
        String ext = "." + type.name();
        ListObjectsRequest request =
            ListObjectsRequest.builder()
                .bucket(getBucket(tenant))
                .prefix(prefix)
                .delimiter(PATH_SEP)
                .build();
        ListObjectsResponse response = s3Client.listObjects(request);
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

    String bucket = getBucket(tenant);
    List<Future<PutObjectResponse>> futures =
        storageObjects.stream().map(pso -> putObject(pso, bucket)).toList();

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

  private Future<PutObjectResponse> putObject(StorageObjectId storageObject, String bucket) {
    // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
    String key = getKey(storageObject.getId(), storageObject.getType());
    PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).build();
    AsyncRequestBody body = getAsyncrequestBody(storageObject);
    return s3AsyncClient.putObject(request, body);
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

  //  @Override
  //  public void putPathStorageObjects(Long tenant, List<PathStorageObjectId> pathStorageObjects)
  //      throws IOException {
  //
  //    String bucket = getBucket(tenant);
  //    List<Future<PutObjectResponse>> futures =
  //        pathStorageObjects.stream().map(pso -> putPathObject(pso, bucket)).toList();
  //
  //    try {
  //      ThreadUtils.joinFutures(futures);
  //    } catch (InterruptedException e) {
  //      Thread.currentThread().interrupt();
  //      throw new IOException(e);
  //    } catch (ExecutionException e) {
  //      if (e.getCause() instanceof IOException ioe) {
  //        throw ioe;
  //      }
  //      throw new IOException(e.getCause());
  //    }
  //  }
  //
  //  private Future<PutObjectResponse> putPathObject(PathStorageObjectId pso, String bucket) {
  //    //
  // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
  //    String key = getKey(pso.getId(), pso.getType());
  //    PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).build();
  //    AsyncRequestBody body = AsyncRequestBody.fromFile(pso.getPath());
  //    return s3AsyncClient.putObject(request, body);
  //  }
  //
  //  @Override
  //  public void putByteStorageObjects(Long tenant, List<ByteStorageObjectId> byteStorageObjects)
  //      throws IOException {
  //
  //    String bucket = getBucket(tenant);
  //    List<Future<PutObjectResponse>> futures =
  //        byteStorageObjects.stream().map(bso -> putBytesObject(bso, bucket)).toList();
  //
  //    try {
  //      ThreadUtils.joinFutures(futures);
  //    } catch (InterruptedException e) {
  //      Thread.currentThread().interrupt();
  //      throw new IOException(e);
  //    } catch (ExecutionException e) {
  //      if (e.getCause() instanceof IOException ioe) {
  //        throw ioe;
  //      }
  //      throw new IOException(e.getCause());
  //    }
  //  }
  //
  //  private Future<PutObjectResponse> putBytesObject(ByteStorageObjectId bso, String bucket) {
  //    String key = getKey(bso.getId(), bso.getType());
  //    PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key).build();
  //    AsyncRequestBody body = AsyncRequestBody.fromBytes(bso.getBytes());
  //    return s3AsyncClient.putObject(request, body);
  //  }

  @Override
  public void putStorageObject(Path srcPath, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
    try {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build(),
          srcPath);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void putStorageObject(byte[] bytes, Long tenant, Long id, StorageObjectType type)
      throws IOException {
    // https://aws.amazon.com/fr/blogs/developer/introducing-crt-based-s3-client-and-the-s3-transfer-manager-in-the-aws-sdk-for-java-2-x/
    try {
      s3Client.putObject(
          PutObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build(),
          RequestBody.fromBytes(bytes));
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void putStorageObject(
      InputStream inputStream, Long tenant, Long id, StorageObjectType type) throws IOException {
    if (inputStream instanceof StorageInputStream storageStream) {
      try {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build(),
            RequestBody.fromInputStream(inputStream, storageStream.getLength()));
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
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build());
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public void deleteStorageObjectIfExists(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build());
    } catch (NoSuchKeyException e) {
      log.warn(
          "Failed to delete object - bucket: %s - key: %s"
              .formatted(getBucket(tenant), getKey(id, type)),
          e);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean existsStorageObject(Long tenant, Long id, StorageObjectType type)
      throws IOException {
    try {
      s3Client.headObject(
          HeadObjectRequest.builder().bucket(getBucket(tenant)).key(getKey(id, type)).build());
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

    try {
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(getBucket(tenant))
              .key(getKey(secureNumber, StorageObjectType.lbk))
              .build(),
          RequestBody.fromInputStream(byteArray.getInputStream(), byteArray.size()));
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
    return type.name() + PATH_SEP + id + "." + type.name();
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
    String bucket = buckets.get(tenant);
    if (bucket == null) {
      synchronized (lock) {
        // We could have ignored the next line (see. java memory barrier) because bucket creation is
        // idempotent
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
          CreateBucketRequest.builder()
              .bucket(bucket)
              .createBucketConfiguration(c -> c.locationConstraint(region.id()))
              .build());

      s3Client.waiter().waitUntilBucketExists(h -> h.bucket(bucket));
    } catch (BucketAlreadyOwnedByYouException exception) {
      log.info("Bucket {} already created", bucket);
    } catch (S3Exception e) {
      throw new IOException(e);
    }
  }

  private record GetObjectFuture(
      String key, CompletableFuture<ResponseBytes<GetObjectResponse>> future) {}
}
