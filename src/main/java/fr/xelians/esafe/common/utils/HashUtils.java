/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import fr.xelians.esafe.common.exception.technical.InternalException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.greenrobot.essentials.hash.Murmur3F;

/*
 * @author Emmanuel Deviller
 */
public final class HashUtils {

  private HashUtils() {}

  public static Hash getHash(String name) {
    return switch (name.toLowerCase()) {
      case "md5", "md-5" -> Hash.MD5;
      case "sha256", "sha-256" -> Hash.SHA256;
      case "sha512", "sha-512" -> Hash.SHA512;
      default -> throw new InternalException(String.format("Unknown digest algorithm %s", name));
    };
  }

  public static boolean isValidDigest(String name) {
    return switch (name.toLowerCase()) {
      case "sha256", "sha-256", "sha512", "sha-512" -> true;
      default -> false;
    };
  }

  public static byte[] decodeHex(String str) {
    try {
      return Hex.decodeHex(str);
    } catch (DecoderException e) {
      throw new InternalException(e);
    }
  }

  public static String encodeHex(byte[] bytes) {
    return Hex.encodeHexString(bytes);
  }

  public static MessageDigest getMessageDigest(Hash hash) throws IOException {
    try {
      return MessageDigest.getInstance(hash.getAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      throw new IOException(e);
    }
  }

  /*
      MD5 (like SHA 256 & 512) is intrinsified and pretty fast.
      cf. https://bugs.openjdk.java.net/browse/JDK-8250902
      cf. https://cl4es.github.io/2021/02/14/Reducing-MD5-and-SHA-Overheads.html
      Note. MD5 is about 3 times faster than SHA512.
  */

  public static byte[] checksum(Hash hash, List<Path> paths) throws IOException {
    final byte[] buffer = new byte[1024];
    MessageDigest digest = DigestUtils.getDigest(hash.getAlgorithm());

    for (Path path : paths) {
      try (InputStream is = Files.newInputStream(path)) {
        int read = is.read(buffer, 0, buffer.length);
        while (read > -1) {
          digest.update(buffer, 0, read);
          read = is.read(buffer, 0, buffer.length);
        }
      }
    }
    return digest.digest();
  }

  public static byte[] checksum(Hash hash, Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return DigestUtils.digest(DigestUtils.getDigest(hash.getAlgorithm()), is);
    }
  }

  public static byte[] checksum(Hash hash, InputStream is) throws IOException {
    return DigestUtils.digest(DigestUtils.getDigest(hash.getAlgorithm()), is);
  }

  public static byte[] checksum(Hash hash, byte[] bytes) {
    return DigestUtils.digest(DigestUtils.getDigest(hash.getAlgorithm()), bytes);
  }

  // Should be faster than MD5 (But need tests to be sure!)
  public static String m3fHex(Path path) throws IOException {
    final int LENGTH = 1024;
    final byte[] buffer = new byte[LENGTH];
    Murmur3F murmur = new Murmur3F();
    try (InputStream is = Files.newInputStream(path)) {
      int read = is.read(buffer, 0, LENGTH);
      while (read > -1) {
        murmur.update(buffer, 0, read);
        read = is.read(buffer, 0, LENGTH);
      }
    }
    long value = murmur.getValue();
    return value < 0 ? "n" + (-value) : "p" + value;
  }
}
