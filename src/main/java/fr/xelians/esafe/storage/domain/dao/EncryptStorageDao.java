/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.dao;

import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.storage.domain.Aes;
import fr.xelians.esafe.storage.domain.StorageObjectType;
import fr.xelians.esafe.storage.domain.object.*;
import fr.xelians.esafe.storage.domain.pack.EncryptFilePacker;
import fr.xelians.esafe.storage.domain.pack.Packer;
import fr.xelians.esafe.storage.service.StorageService;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.*;

public class EncryptStorageDao extends DirectStorageDao {

  private SecretKey secretKey;

  public EncryptStorageDao(StorageService storageService, SecretKey secretKey) {
    super(storageService);
    this.secretKey = secretKey;
  }

  @Override
  public Packer createPacker(Path path) {
    return new EncryptFilePacker(secretKey, path);
  }

  @Override
  public InputStream getObjectStream(
      Long tenant, List<String> offers, Long id, StorageObjectType type) throws IOException {
    InputStream is = super.getObjectStream(tenant, offers, id, type);
    Cipher cipher = Aes.createCipher();
    Aes.initReadCipher(cipher, secretKey, is);
    return new CipherInputStream(is, cipher);
  }

  @Override
  public InputStream getObjectStream(
      Long tenant, List<String> offers, Long id, long start, long end, StorageObjectType type)
      throws IOException {
    InputStream is = super.getObjectStream(tenant, offers, id, start, end, type);
    Cipher cipher = Aes.createCipher();
    Aes.initReadCipher(cipher, secretKey, is);
    return new CipherInputStream(is, cipher);
  }

  @Override
  public byte[] getObjectByte(Long tenant, List<String> offers, Long id, StorageObjectType type)
      throws IOException {
    return Aes.decrypt(super.getObjectByte(tenant, offers, id, type), secretKey);
  }

  // Use with great care: limit the list size to avoid OOM.
  @Override
  public List<byte[]> getObjectBytes(
      Long tenant, List<String> offers, List<StorageObjectId> storageObjectIds) throws IOException {
    Cipher cipher = Aes.createCipher();
    List<byte[]> bytes = new ArrayList<>();
    try {
      for (byte[] obs : super.getObjectBytes(tenant, offers, storageObjectIds)) {
        Aes.initReadCipher(cipher, secretKey, obs);
        bytes.add(cipher.doFinal(Arrays.copyOfRange(obs, 16, obs.length)));
      }
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
    return bytes;
  }

  //  public List<ChecksumStorageObjectId> putPathStorageObjects(
  //      Long tenant, List<String> offers, List<PathStorageObjectId> pathStorageObjectIds)
  //      throws IOException {
  //    Cipher cipher = Aes.createCipher();
  //
  //    List<ChecksumStorageObjectId> csois = new ArrayList<>();
  //    List<PathStorageObjectId> psois = new ArrayList<>();
  //
  //    try {
  //      for (PathStorageObjectId psoi : pathStorageObjectIds) {
  //        if (psoi.isIgnoreEncryption()) {
  //          // This file is already encrypted and does not need further encryption
  //          psois.add(psoi);
  //          if (!psoi.isIgnoreChecksum()) {
  //            csois.add(createChecksumStorageObjectId(psoi));
  //          }
  //        } else {
  //          Path dstPath = NioUtils.createTempFile();
  //          copy(psoi.getPath(), dstPath, cipher);
  //          PathStorageObjectId pathStorageObjectId =
  //              new PathStorageObjectId(dstPath, psoi.getId(), psoi.getType());
  //          psois.add(pathStorageObjectId);
  //          if (!psoi.isIgnoreChecksum()) {
  //            csois.add(createChecksumStorageObjectId(pathStorageObjectId));
  //          }
  //        }
  //      }
  //
  //      for (String offer : offers) {
  //        getStorageOffer(offer).putPathStorageObjects(tenant, psois);
  //      }
  //    } finally {
  //      for (PathStorageObjectId psoi : psois) {
  //        // Delete only temp files
  //        if (!psoi.isIgnoreEncryption()) {
  //          NioUtils.deletePathQuietly(psoi.getPath());
  //        }
  //      }
  //    }
  //    return csois;
  //  }

  @Override
  public List<ChecksumStorageObject> putStorageObjects(
      Long tenant, List<String> offers, List<StorageObject> storageObjects) throws IOException {
    Cipher cipher = Aes.createCipher();

    List<ChecksumStorageObject> csois = new ArrayList<>();
    List<StorageObject> sois = new ArrayList<>();

    try {
      for (StorageObject soi : storageObjects) {
        if (soi.isIgnoreEncryption()) {
          // This file is already encrypted and does not need further encryption
          sois.add(soi);
          if (!soi.isIgnoreChecksum()) {
            csois.add(createChecksumStorageObjectId(soi));
          }
        } else if (soi instanceof PathStorageObject psoi) {
          Path dstPath = NioUtils.createTempFile();
          copy(psoi.getPath(), dstPath, cipher);
          PathStorageObject pathStorageObject =
              new PathStorageObject(dstPath, psoi.getId(), psoi.getType());
          sois.add(pathStorageObject);
          if (!psoi.isIgnoreChecksum()) {
            csois.add(createChecksumStorageObjectId(pathStorageObject));
          }
        } else if (soi instanceof ByteStorageObject bsoi) {
          try {
            // Init initialization vector & cipher
            byte[] ivBytes = Aes.initWriteCipher(cipher, secretKey);

            // Encrypt bytes
            byte[] encBytes = cipher.doFinal(bsoi.getBytes());

            // Prepend initialization vector
            byte[] storageBytes = new byte[encBytes.length + 16];
            System.arraycopy(ivBytes, 0, storageBytes, 0, 16);
            System.arraycopy(encBytes, 0, storageBytes, 16, encBytes.length);

            ByteStorageObject byteStorageObject =
                new ByteStorageObject(storageBytes, bsoi.getId(), bsoi.getType());
            sois.add(byteStorageObject);
            if (!bsoi.isIgnoreChecksum()) {
              csois.add(createChecksumStorageObjectId(byteStorageObject));
            }
          } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new IOException(e);
          }
        }
      }

      for (String offer : offers) {
        getStorageOffer(offer).putStorageObjects(tenant, sois);
      }
    } finally {
      for (StorageObjectId soi : sois) {
        // Delete only temp files
        if (soi instanceof PathStorageObject psoi && !psoi.isIgnoreEncryption()) {
          NioUtils.deletePathQuietly(psoi.getPath());
        }
      }
    }
    return csois;
  }

  //  @Override
  //  public List<ChecksumStorageObjectId> putByteStorageObjects(
  //      Long tenant, List<String> offers, List<ByteStorageObjectId> byteStorageObjectIds)
  //      throws IOException {
  //    Cipher cipher = Aes.createCipher();
  //
  //    List<ChecksumStorageObjectId> csois = new ArrayList<>();
  //    List<ByteStorageObjectId> bsois = new ArrayList<>();
  //
  //    for (ByteStorageObjectId bsoi : byteStorageObjectIds) {
  //      if (bsoi.isIgnoreEncryption()) {
  //        // This file is already encrypted and does not need further encryption
  //        bsois.add(bsoi);
  //        if (!bsoi.isIgnoreChecksum()) {
  //          csois.add(createChecksumStorageObjectId(bsoi));
  //        }
  //      } else {
  //        try {
  //          // Init initialization vector & cipher
  //          byte[] ivBytes = Aes.initWriteCipher(cipher, secretKey);
  //
  //          // Encrypt bytes
  //          byte[] encBytes = cipher.doFinal(bsoi.getBytes());
  //
  //          // Prepend initialization vector
  //          byte[] storageBytes = new byte[encBytes.length + 16];
  //          System.arraycopy(ivBytes, 0, storageBytes, 0, 16);
  //          System.arraycopy(encBytes, 0, storageBytes, 16, encBytes.length);
  //
  //          ByteStorageObjectId byteStorageObjectId =
  //              new ByteStorageObjectId(storageBytes, bsoi.getId(), bsoi.getType());
  //          bsois.add(byteStorageObjectId);
  //          if (!bsoi.isIgnoreChecksum()) {
  //            csois.add(createChecksumStorageObjectId(byteStorageObjectId));
  //          }
  //        } catch (IllegalBlockSizeException | BadPaddingException e) {
  //          throw new IOException(e);
  //        }
  //      }
  //    }
  //
  //    for (String offer : offers) {
  //      getStorageOffer(offer).putByteStorageObjects(tenant, bsois);
  //    }
  //
  //    return csois;
  //  }

  private void copy(Path srcPath, Path dstPath, Cipher cipher) throws IOException {
    byte[] buffer = new byte[8192];

    // Init initialization vector & cipher
    byte[] ivBytes = Aes.initWriteCipher(cipher, secretKey);

    // Encrypt stream
    try (InputStream is = Files.newInputStream(srcPath);
        OutputStream os = Files.newOutputStream(dstPath, StandardOpenOption.WRITE);
        CipherOutputStream cos = new CipherOutputStream(os, cipher)) {

      // Prepend initialization vector
      os.write(ivBytes);

      // Encrypt bytes
      int length;
      while ((length = is.read(buffer)) > 0) {
        cos.write(buffer, 0, length);
      }
    }
  }

  @Override
  public void close() {
    secretKey = null;
  }
}
