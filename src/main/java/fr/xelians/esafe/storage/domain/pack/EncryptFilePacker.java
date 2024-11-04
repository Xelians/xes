/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.pack;

import fr.xelians.esafe.storage.domain.GcmCipher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.crypto.SecretKey;
import org.apache.commons.lang3.Validate;

// This class is not thread safe
/*
 * @author Emmanuel Deviller
 */
public class EncryptFilePacker implements Packer {

  private final Path dstPath;
  private OutputStream dstStream;
  private long end;
  private GcmCipher cipher;

  public EncryptFilePacker(SecretKey secretKey, Path dstPath) {
    Validate.notNull(dstPath, "dstPath");
    this.dstPath = dstPath;
    this.cipher = GcmCipher.create(secretKey);
  }

  @Override
  public long[] write(Path srcPath) throws IOException {
    if (dstStream == null) {
      dstStream = Files.newOutputStream(dstPath);
    }
    return doWrite(srcPath);
  }

  private long[] doWrite(Path srcPath) throws IOException {
    long start = end;
    long size;

    try (InputStream srcStream = Files.newInputStream(srcPath)) {
      size = cipher.encrypt(srcStream, dstStream);
    }

    if (size > 0) {
      end = start + size;
      return new long[] {start, end - 1};
    }
    return new long[] {-1, -1};
  }

  @Override
  public void close() throws IOException {
    cipher = null;
    if (dstStream != null) {
      dstStream.close();
    }
  }
}
