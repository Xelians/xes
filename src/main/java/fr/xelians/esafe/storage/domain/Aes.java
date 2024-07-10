/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class Aes {

  // !!!! Do not change the following attributes !!!!
  public static final String AES_ALGORITHM = "AES";
  public static final String CIPHER_ALGORITM = "AES/CBC/PKCS5Padding";
  public static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final byte[] SALT = new byte[] {56, 16, -15, 89, 65, -106, -17, 77};
  public static final int BLOCK_SIZE = 16;
  // !!!! Do not change the preceding attributes !!!!

  private static final SecureRandom random = new SecureRandom();

  private Aes() {}

  public static SecretKey createSecretKey(String password) {
    try {
      SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
      KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT, 65536, 256);
      SecretKey tmp = factory.generateSecret(spec);
      return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
      throw new InternalException(
          "AES secret initialisation failed", "Failed to create secret key from key generator", ex);
    }
  }

  public static SecretKey createSecretKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
      keyGenerator.init(256);
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException ex) {
      throw new InternalException(
          "AES secret initialisation failed", "Failed to create secret key from key generator", ex);
    }
  }

  public static Cipher createCipher() throws IOException {
    try {
      return Cipher.getInstance(CIPHER_ALGORITM);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IOException(e);
    }
  }

  public static byte[] initWriteCipher(Cipher cipher, SecretKey secretKey) throws IOException {
    // Init initialization vector
    byte[] ivBytes = new byte[BLOCK_SIZE];
    random.nextBytes(ivBytes);

    // Init cipher
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    }
    return ivBytes;
  }

  public static void initReadCipher(Cipher cipher, SecretKey secretKey, InputStream is)
      throws IOException {
    byte[] ivBytes = is.readNBytes(BLOCK_SIZE);
    if (ivBytes.length != BLOCK_SIZE) {
      throw new IOException("Failed to read initialisation vector from input stream");
    }

    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    }
  }

  public static void initReadCipher(Cipher cipher, SecretKey secretKey, byte[] bytes)
      throws IOException {
    if (bytes.length < BLOCK_SIZE) {
      throw new IOException("Failed to read initialisation vector from bytes");
    }

    byte[] ivBytes = Arrays.copyOfRange(bytes, 0, BLOCK_SIZE);
    try {
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));
    } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
      throw new IOException(e);
    }
  }

  public static byte[] decrypt(byte[] bytes, SecretKey secretKey) throws IOException {
    Cipher cipher = Aes.createCipher();
    Aes.initReadCipher(cipher, secretKey, bytes);
    try {
      return cipher.doFinal(Arrays.copyOfRange(bytes, BLOCK_SIZE, bytes.length));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
  }

  public static byte[] encrypt(byte[] bytes, SecretKey secretKey) throws IOException {
    Cipher cipher = createCipher();
    byte[] ivBytes = initWriteCipher(cipher, secretKey);
    try {
      byte[] encBytes = cipher.doFinal(bytes);
      byte[] storageBytes = new byte[encBytes.length + BLOCK_SIZE];
      System.arraycopy(ivBytes, 0, storageBytes, 0, BLOCK_SIZE);
      System.arraycopy(encBytes, 0, storageBytes, BLOCK_SIZE, encBytes.length);
      return storageBytes;
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      throw new IOException(e);
    }
  }
}
