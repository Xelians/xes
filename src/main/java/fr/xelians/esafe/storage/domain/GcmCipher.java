/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

// This class is not thread safe
/*
 * @author Emmanuel Deviller
 */
public final class GcmCipher {

  public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
  private static final int TAG_LENGTH = 128;
  private static final int IV_SIZE = 12;
  private static final SecureRandom random = new SecureRandom();

  private final Cipher cipher;
  private final SecretKey secretKey;

  private GcmCipher(Cipher cypher, SecretKey secretKey) {
    this.cipher = cypher;
    this.secretKey = secretKey;
  }

  public static GcmCipher create(SecretKey secretKey) {
    try {
      return new GcmCipher(Cipher.getInstance(CIPHER_ALGORITHM), secretKey);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new InternalException(e);
    }
  }

  public byte[] encrypt(byte[] data) throws IOException {
    byte[] ivBytes = initEncryptCipher();
    try {
      byte[] encryptedBytes = cipher.doFinal(data);
      byte[] encryptedData = new byte[1 + IV_SIZE + encryptedBytes.length];
      encryptedData[0] = (byte) ivBytes.length;
      System.arraycopy(ivBytes, 0, encryptedData, 1, IV_SIZE);
      System.arraycopy(encryptedBytes, 0, encryptedData, IV_SIZE + 1, encryptedBytes.length);
      return encryptedData;
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
  }

  public long encrypt(InputStream inputStream, OutputStream outputStream) throws IOException {

    byte[] ivBytes = initEncryptCipher();

    // Prepend initialization vector
    outputStream.write((byte) ivBytes.length);
    outputStream.write(ivBytes);
    long size = 1L + ivBytes.length;

    // Encrypt stream
    byte[] buffer = new byte[8192];
    byte[] obuffer = null;
    int len;

    try {
      while ((len = inputStream.read(buffer)) != -1) {
        obuffer = getOutputBuffer(len, obuffer);
        int ostored;
        if (obuffer != null && obuffer.length > 0) {
          ostored = cipher.update(buffer, 0, len, obuffer);
        } else {
          obuffer = cipher.update(buffer, 0, len);
          ostored = (obuffer != null) ? obuffer.length : 0;
        }
        if (ostored > 0) {
          outputStream.write(obuffer, 0, ostored);
          size += ostored;
        }
      }

      obuffer = getOutputBuffer(0, obuffer);
      int ostored;
      if (obuffer != null && obuffer.length > 0) {
        ostored = cipher.doFinal(obuffer, 0);
      } else {
        obuffer = cipher.doFinal();
        ostored = (obuffer != null) ? obuffer.length : 0;
      }
      if (ostored > 0) {
        outputStream.write(obuffer, 0, ostored);
        size += ostored;
      }

    } catch (IllegalBlockSizeException | BadPaddingException | ShortBufferException e) {
      throw new IOException(e);
    }
    return size;
  }

  private byte[] getOutputBuffer(int inLen, byte[] obuffer) {
    int minLen = cipher.getOutputSize(inLen);
    return obuffer != null && obuffer.length < minLen ? new byte[minLen] : obuffer;
  }

  public InputStream decrypt(InputStream encryptedInputStream) throws IOException {
    initDecryptCipher(encryptedInputStream);
    return new CipherInputStream(encryptedInputStream, cipher);
  }

  public byte[] decrypt(byte[] encryptedData) throws IOException {
    initDecryptCipher(encryptedData);
    try {
      return cipher.doFinal(encryptedData, 1 + IV_SIZE, encryptedData.length - (1 + IV_SIZE));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
  }

  private byte[] initEncryptCipher() throws IOException {
    // For GCM a 12 byte random (or counter) byte-array is recommended by NIST
    // The initialization vector must NEVER be reused in GCM
    byte[] ivBytes = new byte[IV_SIZE];
    random.nextBytes(ivBytes);

    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, ivBytes));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    }
    return ivBytes;
  }

  private void initDecryptCipher(InputStream encryptedInputStream) throws IOException {
    byte[] encryptedData = encryptedInputStream.readNBytes(IV_SIZE + 1);
    initDecryptCipher(encryptedData);
  }

  private void initDecryptCipher(byte[] encryptedData) throws IOException {

    int ivSize = encryptedData[0];
    if (ivSize != IV_SIZE) {
      throw new IllegalStateException("Unexpected initialisation vector length");
    }

    if (encryptedData.length < IV_SIZE + 1) {
      throw new IOException("Failed to read initialisation vector from bytes");
    }

    try {
      cipher.init(
          Cipher.DECRYPT_MODE,
          secretKey,
          new GCMParameterSpec(TAG_LENGTH, encryptedData, 1, IV_SIZE));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    }
  }
}
