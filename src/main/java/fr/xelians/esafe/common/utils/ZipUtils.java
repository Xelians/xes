/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import lombok.SneakyThrows;

/*
 * @author Emmanuel Deviller
 */
public final class ZipUtils {

  private ZipUtils() {}

  public static void unzip(final Path zipPath, Path dstPath) throws IOException {
    int count = 0;
    long size = 0;

    dstPath = dstPath.normalize();

    try (InputStream is = Files.newInputStream(zipPath);
        ZipInputStream zis = new ZipInputStream(is)) {

      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        count++;

        // Avoid zip bomb
        if (count > 20_000L) {
          throw new IOException(String.format("Failed to unzip %s. Too many files", zipPath));
        }

        // Avoid zip slip
        Path path = dstPath.resolve(zipEntry.getName()).normalize();
        if (!path.startsWith(dstPath)) {
          throw new IOException(
              String.format(
                  "Failed to unzip %s. Extracted path %s is not child of %s",
                  zipPath, path, dstPath));
        }

        if (zipEntry.isDirectory()) {
          Files.createDirectories(path);
        } else {
          size += Files.copy(zis, path);
          if (size > 100_000_000_000L) {
            throw new IOException(String.format("Failed to unzip %s. File is too big", zipPath));
          }
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
    }
  }

  public static void unzip2(String filename, Path dirPath) throws IOException {
    Path dstPath = dirPath.normalize();
    AtomicInteger count = new AtomicInteger();
    AtomicLong size = new AtomicLong();
    try (ZipFile zipFile = new ZipFile(filename)) {
      if (Files.size(Path.of(filename)) > 10_000_000L && zipFile.size() > 3) {
        zipFile.stream()
            .parallel() // enable multi-threading
            .forEach(e -> unzipEntry(zipFile, e, dstPath, count, size));
      } else {
        zipFile.stream().forEach(e -> unzipEntry(zipFile, e, dstPath, count, size));
      }
    }
  }

  @SneakyThrows
  private static void unzipEntry(
      ZipFile zipFile, ZipEntry entry, Path dstPath, AtomicInteger count, AtomicLong size) {

    // Avoid zip bomb
    if (count.incrementAndGet() > 20_000L) {
      throw new IOException(String.format("Failed to unzip %s. Too many files", zipFile));
    }

    // Avoid zip slip
    Path path = dstPath.resolve(entry.getName()).normalize();
    if (!path.startsWith(dstPath)) {
      throw new IOException(
          String.format(
              "Failed to unzip %s. Extracted path %s is not child of %s", zipFile, path, dstPath));
    }

    if (Files.isDirectory(path)) {
      Files.createDirectories(path);
    } else {
      Files.createDirectories(path.getParent());
      try (InputStream in = zipFile.getInputStream(entry)) {
        long delta = Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        if (size.addAndGet(delta) > 100_000_000_000L) {
          throw new IOException(String.format("Failed to unzip %s. File is too big", zipFile));
        }
      }
    }
  }
}
