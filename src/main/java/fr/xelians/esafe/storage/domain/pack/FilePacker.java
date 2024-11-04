/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain.pack;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.Validate;

// This class is optimized to improve performance on the default file system.
// The destination path determines the type of file system to use.
// If the default file system is used, then all source paths must rely on this one.
/*
 * @author Emmanuel Deviller
 */
public class FilePacker implements Packer {

  private final Path dstPath;
  private final boolean onFS;
  private OutputStream dstStream;
  private long end;

  public FilePacker(Path dstPath) {
    Validate.notNull(dstPath, "dstPath");
    this.dstPath = dstPath;
    this.onFS = dstPath.getFileSystem() == FileSystems.getDefault();
  }

  @Override
  public long[] write(Path srcPath) throws IOException {
    if (dstStream == null) {
      dstStream = onFS ? new FileOutputStream(dstPath.toFile()) : Files.newOutputStream(dstPath);
    }
    return onFS ? doWrite(srcPath.toFile()) : doWrite(srcPath);
  }

  private long[] doWrite(Path srcPath) throws IOException {
    long start = end;
    long size;
    try (InputStream is = Files.newInputStream(srcPath)) {
      size = is.transferTo(dstStream);
    }
    if (size > 0) {
      end = start + size;
      return new long[] {start, end - 1};
    }
    return new long[] {-1, -1};
  }

  // Warning : we don't differentiate absent (error) files and empty (weird but normal) files
  private long[] doWrite(File file) throws IOException {
    long start = end;
    long size;
    try (FileInputStream fis = new FileInputStream(file)) {
      FileChannel srcChannel = fis.getChannel();
      FileChannel dstChannel = ((FileOutputStream) dstStream).getChannel();
      size = srcChannel.transferTo(0, srcChannel.size(), dstChannel);
    }
    if (size > 0) {
      end = start + size;
      return new long[] {start, end - 1};
    }
    return new long[] {-1, -1};
  }

  @Override
  public void close() throws IOException {
    if (dstStream != null) {
      dstStream.close();
    }
  }
}
